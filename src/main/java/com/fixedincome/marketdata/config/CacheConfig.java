package com.fixedincome.marketdata.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  @Primary
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    
    // Default cache configuration - 4 hours TTL for most market data
    cacheManager.setCaffeine(Caffeine.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(Duration.ofHours(4))
      .recordStats());
    
    // Define cache names with appropriate TTLs
    cacheManager.setCacheNames(java.util.Arrays.asList(
      "creditSpreads",         // Credit spreads - 4h (default)
      "inflationExpectations", // Inflation data - 4h (default) 
      "benchmarkRates",        // Benchmark rates - 4h (default)
      "sectorCreditData",      // Sector credit - 4h (default)
      "liquidityPremiums",     // Liquidity premiums - 4h (default)
      "marketData"             // General market data - 4h (default)
    ));
    
    return cacheManager;
  }
  
  @Bean("yieldCurveCacheManager")
  public CacheManager yieldCurveCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    
    // Yield curves update daily, cache for 24 hours
    cacheManager.setCaffeine(Caffeine.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(Duration.ofHours(24))
      .recordStats());
    
    cacheManager.setCacheNames(java.util.Arrays.asList(
      "yieldCurves",
      "yieldCurvesBatch"
    ));
    
    return cacheManager;
  }

  @Bean("timeSeriesCacheManager") 
  public CacheManager timeSeriesCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    
    // Time series data is historical - cache longer (7 days)
    cacheManager.setCaffeine(Caffeine.newBuilder()
      .maximumSize(200)
      .expireAfterWrite(Duration.ofDays(7))
      .recordStats());
    
    cacheManager.setCacheNames(java.util.Arrays.asList(
      "yieldTimeSeries"
    ));
    
    return cacheManager;
  }
}
