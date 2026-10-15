package com.mybudget.server.services.mongo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MongoSyncrService {

    // short-lived secrets: never replicated to Atlas and never restored from it
    private static final Set<String> EXCLUDED_COLLECTIONS = Set.of("verification_tokens");

    private final MongoTemplate localMongoTemplate;
    private final MongoTemplate atlasMongoTemplate;

    @Value("${app.sync.enabled:true}")
    private boolean syncEnable;

    public MongoSyncrService(
            @Qualifier("mongoTemplate") MongoTemplate localMongoTemplate,
            @Qualifier("atlasMongoTemplate") MongoTemplate atlasMongoTemplate
    ) {
        this.localMongoTemplate = localMongoTemplate;
        this.atlasMongoTemplate = atlasMongoTemplate;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour.
    public void syncAllCollectionsAutomatically() {
        if (!syncEnable) return;

        Set<String> collectionNames = localMongoTemplate.getCollectionNames();

        for (String collectionName : collectionNames) {
            if (isSkipped(collectionName)) {
                continue;
            }

            List<Object> localObjects = localMongoTemplate.findAll(Object.class, collectionName);

            // upsert every document into Atlas under the same collection name
            for (Object obj : localObjects) {
                atlasMongoTemplate.save(obj, collectionName);
            }
        }
    }

    public String pullAllfromAtlasToLocal() {
        if (!syncEnable) {
            return "Sync is disabled";
        }

        Set<String> atlasCollections = atlasMongoTemplate.getCollectionNames();
        int pulledCollections = 0;

        for (String collectionName : atlasCollections) {
            if (isSkipped(collectionName)) {
                continue;
            }
            List<Object> cloudObjects = atlasMongoTemplate.findAll(Object.class, collectionName);

            for (Object obj : cloudObjects) {
                localMongoTemplate.save(obj, collectionName);
            }
            pulledCollections++;
        }
        return "Pulled " + pulledCollections + " successfully from Atlas";
    }

    private boolean isSkipped(String collectionName) {
        // system collections (system.views, system.profile...) and the token collection
        return collectionName.startsWith("system.") || EXCLUDED_COLLECTIONS.contains(collectionName);
    }
}