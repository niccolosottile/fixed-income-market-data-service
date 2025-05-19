package com.fixedincome.marketdata.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    
    // Define your cache names
    cacheManager.setCacheNames(java.util.Arrays.asList(
      "yieldCurves",
      "marketData",
      "bondPrices"
    ));
    
    return cacheManager;
  }
}
