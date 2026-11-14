package com.mybudget.server.config;

import com.mybudget.server.modules.User;
import com.mybudget.server.modules.VerificationToken;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class MongoIndexInitializer {
    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(@Qualifier("mongoTemplate") MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void createIndexes() {
        ensure(VerificationToken.class, new Index().on("tokenHash", Sort.Direction.ASC).unique());
        // TTL index: MongoDB deletes a token ducumet once expiresAt has passed
        ensure(VerificationToken.class, new Index().on("expiresAt", Sort.Direction.ASC).expire(0));
        ensure(VerificationToken.class, new Index().on("username", Sort.Direction.ASC).on("type", Sort.Direction.ASC));
        // prevents duplicate registrations of the same email
        ensure(User.class, new Index().on("username", Sort.Direction.ASC).unique());
    }

    private void ensure(Class<?> type, Index index){
        try {
            mongoTemplate.indexOps(type).ensureIndex(index);
        } catch (Exception e){
            log.warn("Could not create index {} on {}: {}", index.getIndexKeys(), type.getSimpleName(), e.getMessage());        }
    }
}
