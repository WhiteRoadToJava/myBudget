package com.mybudget.server.services.mongo;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MongoSyncrService {
    private final MongoTemplate localMongoTemplate;
    private final MongoTemplate atlasMongoTemplate;


    public MongoSyncrService(
            @Qualifier("mongoTemplate") MongoTemplate localMongoTemplate,
            @Qualifier("atlasMongoTemplate") MongoTemplate atlasMongoTemplate
    ){
        this.localMongoTemplate = localMongoTemplate;
        this.atlasMongoTemplate = atlasMongoTemplate;
    }



    @Scheduled(fixedRate = 3000) // Runs every 5 minutes
    public void syncAllCollectionsAutomatically() {
        // 1. Automatically fetch ALL existing collection names from the local database
        Set<String> collectionNames = localMongoTemplate.getCollectionNames();

        for (String collectionName : collectionNames) {
            // Skip system collections (e.g., system.views, system.profile)
            if (collectionName.startsWith("system.")) {
                continue;
            }

            // 2. Fetch documents from the local collection
            List<Object> localObjects = localMongoTemplate.findAll(Object.class, collectionName);

            // 3. Upsert (save/update) every document into Atlas under the same collection name
            for (Object obj : localObjects) {
                atlasMongoTemplate.save(obj, collectionName);
            }
        }
    }

    public String pullAllfromAtlasToLocal(){
        Set<String> atlasCollections = atlasMongoTemplate.getCollectionNames();
        int pulledCollections = 0;
        for(String collectionName : atlasCollections){
            if(collectionName.startsWith("system.")){
                continue;
            }
            List<Object> cloadObjects = atlasMongoTemplate.findAll(Object.class, collectionName);

            for(Object obj : cloadObjects){
                localMongoTemplate.save(obj, collectionName);
            }
            pulledCollections++;
        }
        return  "Pulled " + pulledCollections + " successfully from Atlas";
    }
}
