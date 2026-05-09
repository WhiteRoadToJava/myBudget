package com.mybudget.server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class CacheConnfig {
    @Bean
    public CacheManager manager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

                manager.registerCustomCache("accounts,",
                        Caffeine.newBuilder()
                                .maximumSize(1)
                                .expireAfterWrite(1, TimeUnit.HOURS)
                                .recordStats()
                                .build());


        manager.registerCustomCache("account",
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .recordStats()
                        .build());

        manager.registerCustomCache("incomses",
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .recordStats()
                        .build());

        manager.registerCustomCache("expenses",
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .recordStats()
                        .build());

        manager.registerCustomCache("transfers",
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .recordStats()
                        .build());




return  manager;
    }
}
