package com.fixedincome.marketdata.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centralized cache management service that handles operations across all cache managers.
 * This service abstracts cache clearing and management operations.
 */
@Service
public class CacheManagementService {

  private static final Logger logger = LoggerFactory.getLogger(CacheManagementService.class);

  private final List<CacheManager> cacheManagers;

  public CacheManagementService(List<CacheManager> cacheManagers) {
    this.cacheManagers = cacheManagers;
    logger.info("CacheManagementService initialized with {} cache managers", cacheManagers.size());
  }

  /**
   * Clear all caches across all cache managers
   */
  public void clearAllCaches() {
    logger.info("Clearing all market data caches across {} cache managers...", cacheManagers.size());
    
    int totalCachesCleared = 0;
    
    for (CacheManager cacheManager : cacheManagers) {
      if (cacheManager != null) {
        int cachesCleared = clearCacheManager(cacheManager);
        totalCachesCleared += cachesCleared;
      }
    }
    
    logger.info("Cleared {} caches total - next requests will hit database/providers", totalCachesCleared);
  }

  /**
   * Clear a specific cache by name across all cache managers
   */
  public void clearCacheByName(String cacheName) {
    logger.info("Clearing cache '{}' across all cache managers...", cacheName);
    
    boolean found = false;
    for (CacheManager cacheManager : cacheManagers) {
      if (cacheManager != null) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
          cache.clear();
          logger.debug("Cleared cache '{}' from cache manager: {}", cacheName, cacheManager.getClass().getSimpleName());
          found = true;
        }
      }
    }
    
    if (!found) {
      logger.warn("Cache '{}' not found in any cache manager", cacheName);
    } else {
      logger.info("Cache '{}' cleared successfully", cacheName);
    }
  }

  /**
   * Clear caches by category (e.g., all yield curve related caches)
   */
  public void clearCachesByCategory(String category) {
    logger.info("Clearing caches by category: {}", category);
    
    int totalCleared = 0;
    for (CacheManager cacheManager : cacheManagers) {
      if (cacheManager != null) {
        for (String cacheName : cacheManager.getCacheNames()) {
          if (cacheName.toLowerCase().contains(category.toLowerCase())) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
              cache.clear();
              logger.debug("Cleared cache '{}' (category: {})", cacheName, category);
              totalCleared++;
            }
          }
        }
      }
    }
    
    logger.info("Cleared {} caches in category '{}'", totalCleared, category);
  }

  /**
   * Get cache statistics across all cache managers
   */
  public CacheStatistics getCacheStatistics() {
    int totalCacheManagers = cacheManagers.size();
    int totalCaches = 0;
    
    for (CacheManager cacheManager : cacheManagers) {
      if (cacheManager != null) {
        totalCaches += cacheManager.getCacheNames().size();
      }
    }
    
    return new CacheStatistics(totalCacheManagers, totalCaches, getAllCacheNames());
  }

  /**
   * Get all cache names across all cache managers
   */
  public List<String> getAllCacheNames() {
    return cacheManagers.stream()
      .filter(cm -> cm != null)
      .flatMap(cm -> cm.getCacheNames().stream())
      .distinct()
      .sorted()
      .toList();
  }

  /**
   * Check if a specific cache exists
   */
  public boolean cacheExists(String cacheName) {
    return cacheManagers.stream()
      .filter(cm -> cm != null)
      .anyMatch(cm -> cm.getCache(cacheName) != null);
  }

  /**
   * Private helper method to clear all caches in a specific cache manager
   */
  private int clearCacheManager(CacheManager cacheManager) {
    int cachesCleared = 0;
    String managerName = cacheManager.getClass().getSimpleName();
    
    for (String cacheName : cacheManager.getCacheNames()) {
      var cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        logger.debug("Cleared cache '{}' from {}", cacheName, managerName);
        cachesCleared++;
      }
    }
    
    if (cachesCleared > 0) {
      logger.debug("Cleared {} caches from {}", cachesCleared, managerName);
    }
    
    return cachesCleared;
  }

  /**
   * Simple data class for cache statistics
   */
  public static class CacheStatistics {
    private final int totalCacheManagers;
    private final int totalCaches;
    private final List<String> cacheNames;

    public CacheStatistics(int totalCacheManagers, int totalCaches, List<String> cacheNames) {
      this.totalCacheManagers = totalCacheManagers;
      this.totalCaches = totalCaches;
      this.cacheNames = cacheNames;
    }

    public int getTotalCacheManagers() { return totalCacheManagers; }
    public int getTotalCaches() { return totalCaches; }
    public List<String> getCacheNames() { return cacheNames; }
  }
}