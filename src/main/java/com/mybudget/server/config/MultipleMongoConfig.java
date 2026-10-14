 package com.mybudget.server.config;


import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;


 @Configuration
public class MultipleMongoConfig {
     @Value("${spring.data.mongodb.local.uri}")
     private String localUrl;

     @Value("${spring.data.mongodb.atlas.uri}")
     private String atlasUrl;

     @Value("${app.mongodb.local.database:myBudget_db}")
     private String localDb;

     @Value("${app.mongodb.atlas.database:mybudget}")
     private String atlasDb;

     @Primary
     @Bean(name = "mongoTemplate")
     public MongoTemplate localMongoTemplate() {
         return new MongoTemplate(new SimpleMongoClientDatabaseFactory(MongoClients.create(localUrl), localDb));
     }

     @Bean(name = "atlasMongoTemplate")
     public MongoTemplate atlasMongoTemplate() {
         return new MongoTemplate(new SimpleMongoClientDatabaseFactory(MongoClients.create(atlasUrl), atlasDb));
     }
}
