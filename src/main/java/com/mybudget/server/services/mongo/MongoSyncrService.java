package com.mybudget.server.services.mongo;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Keeps the Atlas database a copy of the local one.
 *
 * - Documents are copied as raw BSON: no conversion to entities, so @DBRef fields
 *   are not resolved and nothing is changed on the way.
 * - Every document is written on its own: one bad document is logged and the
 *   rest of the collection and the other collections still sync.
 * - Push also removes from Atlas what was deleted locally, otherwise a later
 *   pull would bring deleted transactions back and break the balances.
 *   This is skipped for an empty local collection, so a fresh or broken local
 *   database never wipes the copy in Atlas.
 * - Every run is written to the log, so a failing sync can be seen.
 */
@Slf4j
@Service
public class MongoSyncrService {

    // short-lived secrets: never replicated to Atlas and never restored from it
    private static final Set<String> EXCLUDED_COLLECTIONS = Set.of("verification_tokens");

    private final MongoTemplate localMongoTemplate;
    private final MongoTemplate atlasMongoTemplate;

    @Value("${app.sync.enabled:true}")
    private boolean syncEnable;

    private volatile SyncReport lastReport;

    public MongoSyncrService(
            @Qualifier("mongoTemplate") MongoTemplate localMongoTemplate,
            @Qualifier("atlasMongoTemplate") MongoTemplate atlasMongoTemplate
    ) {
        this.localMongoTemplate = localMongoTemplate;
        this.atlasMongoTemplate = atlasMongoTemplate;
    }

    // every hour, first run one minute after start (not while the app is starting)
    @Scheduled(fixedRateString = "${app.sync.rate-ms:3600000}", initialDelayString = "${app.sync.initial-delay-ms:60000}")
    public void syncAllCollectionsAutomatically() {
        if (!syncEnable) {
            log.info("Mongo sync is disabled (APP_SYNC_ENABLED=false)");
            return;
        }
        pushLocalToAtlas();
    }

    public SyncReport pushLocalToAtlas() {
        return run("local -> Atlas", localMongoTemplate, atlasMongoTemplate, true);
    }

    // restore: copies Atlas into local, never deletes local documents
    public SyncReport pullAllfromAtlasToLocal() {
        return run("Atlas -> local", atlasMongoTemplate, localMongoTemplate, false);
    }

    public SyncReport getLastReport() {
        return lastReport;
    }

    private synchronized SyncReport run(String direction, MongoTemplate from, MongoTemplate to, boolean mirrorDeletes) {
        SyncReport report = new SyncReport(direction);
        if (!syncEnable) {
            report.abort("Sync is disabled (APP_SYNC_ENABLED=false)");
            return finish(report);
        }

        Set<String> collections;
        try {
            collections = from.getCollectionNames();
            to.getCollectionNames(); // fails fast when the other side is unreachable
        } catch (RuntimeException e) {
            // wrong URI or password, IP not allowed in Atlas, network down...
            report.abort("Database not reachable: " + e.getMessage());
            return finish(report);
        }

        for (String name : collections) {
            if (isSkipped(name)) {
                continue;
            }
            try {
                copyCollection(from.getCollection(name), to.getCollection(name), name, report, mirrorDeletes);
                report.collectionDone();
            } catch (RuntimeException e) {
                report.failed(name + ": " + e.getMessage());
            }
        }
        return finish(report);
    }

    private void copyCollection(MongoCollection<Document> source, MongoCollection<Document> target,
                                String name, SyncReport report, boolean mirrorDeletes) {
        List<Object> ids = new ArrayList<>();
        ReplaceOptions upsert = new ReplaceOptions().upsert(true);

        for (Document document : source.find()) {
            Object id = document.get("_id");
            ids.add(id);
            try {
                target.replaceOne(Filters.eq("_id", id), document, upsert);
                report.copied();
            } catch (MongoException e) {
                // e.g. a unique index (username) already used by another _id on the target
                report.failed(name + "/" + id + ": " + e.getMessage());
            }
        }

        if (mirrorDeletes && !ids.isEmpty()) {
            report.deleted(target.deleteMany(Filters.nin("_id", ids)).getDeletedCount());
        }
    }

    private SyncReport finish(SyncReport report) {
        lastReport = report;
        if (report.isAborted() || report.getFailed() > 0) {
            log.error("Mongo sync {}", report);
        } else {
            log.info("Mongo sync {}", report);
        }
        return report;
    }

    private boolean isSkipped(String collectionName) {
        // system collections (system.views, system.profile...) and the token collection
        return collectionName.startsWith("system.") || EXCLUDED_COLLECTIONS.contains(collectionName);
    }
}