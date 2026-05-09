package com.mybudget.server.status;


import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CacheStatsLogger {
    private  final CacheManager cacheManager;

            @Scheduled(fixedDelay = 60_000)
    public void logCacheStats(){
                List<String> cacheNames = List.of("account", "accounts", "incomses", "expenses", "transfers");


                cacheNames.forEach(name -> {
                    CaffeineCache cache = (CaffeineCache) cacheManager.getCache(name);
                    if (cache != null) {
                        CacheStats stats = cache.getNativeCache().stats();
                        System.out.printf("""
                    ─── Cache: [%s] ───
                        Hit Count   : %d
                        Miss Count  : %d
                        Hit Rate    : %.1f%%
                        Evictions   : %d
                    %n""",
                                name,
                                stats.hitCount(),
                                stats.missCount(),
                                stats.hitRate() * 100,
                                stats.evictionCount()
                        );
                    }
                });
            }
}
