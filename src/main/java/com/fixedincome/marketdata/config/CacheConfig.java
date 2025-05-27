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
    
    // Define cache names
    cacheManager.setCacheNames(java.util.Arrays.asList(
      "yieldCurves",           // FRED data (24h updates) - will override TTL
      "creditSpreads",         // More frequent updates (4h default)
      "inflationExpectations", // Medium frequency updates
      "benchmarkRates",        // Daily updates
      "sectorCreditData",      // Medium frequency updates
      "liquidityPremiums",     // Low frequency updates
      "yieldTimeSeries",       // Historical data - can cache longer
      "yieldCurvesBatch",      // Batch historical data
      "marketData",            // General market data
      "bondPrices"            // Calculated bond prices
    ));
    
    return cacheManager;
  }
  
  @Bean("yieldCurveCacheManager")
  public CacheManager yieldCurveCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("yieldCurves");
    
    // FRED yield curves update daily, so cache for 24 hours
    cacheManager.setCaffeine(Caffeine.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(Duration.ofHours(24))
      .recordStats());
    
    return cacheManager;
  }
}
