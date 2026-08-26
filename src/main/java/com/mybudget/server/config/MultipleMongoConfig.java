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

    @Primary
     @Bean(name = "mongoTemplate")
     public MongoTemplate localMongoTemplate(){
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(MongoClients.create(localUrl),"myBudget_db"));
    }

    @Bean(name ="atlasMongoTemplate")
     public MongoTemplate atlasMongoTemplate(){
        return  new MongoTemplate(new SimpleMongoClientDatabaseFactory(MongoClients.create(atlasUrl),"mybudget"));
    }
    
}
