package com.mybudget.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

public class MongoConfig {
    @Bean
    public MongoTransactionManager transactionManager (MongoDatabaseFactory doFactory){
        return new MongoTransactionManager(doFactory);
    }

}
