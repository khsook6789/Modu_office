package com.modu.office.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    // 캐시 이름 상수
    public static final String FACILITIES_ACTIVE = "facilities:active";
    public static final String FACILITIES_ALL    = "facilities:all";
    public static final String FACILITY          = "facility";
    public static final String OFFICES           = "offices";
    public static final String OFFICE            = "office";
    public static final String REVIEW_SUMMARY    = "review:summary";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache(FACILITIES_ACTIVE, 6, TimeUnit.HOURS,  200),
                buildCache(FACILITIES_ALL,    6, TimeUnit.HOURS,  200),
                buildCache(FACILITY,          6, TimeUnit.HOURS,  500),
                buildCache(OFFICES,           1, TimeUnit.HOURS,  200),
                buildCache(OFFICE,            1, TimeUnit.HOURS,  500),
                buildCache(REVIEW_SUMMARY,   10, TimeUnit.MINUTES, 1000)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, long maxSize) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .expireAfterWrite(duration, unit)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build());
    }
}
