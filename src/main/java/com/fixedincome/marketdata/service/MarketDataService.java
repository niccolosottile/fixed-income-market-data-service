package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;
import com.fixedincome.marketdata.service.integration.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Centralized market data service that coordinates between cache, database, providers
 * and provides fallback data for valuation calculations.
 * This is the main service used internally by the API for fetching any market data.
 */
@Service
public class MarketDataService {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

  private final MarketDataDatabaseService databaseService;
  private final MarketDataProviderService providerService;
  private final MarketDataFallbackService fallbackService;
  private final MarketDataProvider fredProvider; // For metadata operations

  public MarketDataService(
    MarketDataDatabaseService databaseService,
    MarketDataProviderService providerService,
    MarketDataFallbackService fallbackService,
    @Qualifier("fredApiClient") MarketDataProvider fredProvider) {
    this.databaseService = databaseService;
    this.providerService = providerService;
    this.fallbackService = fallbackService;
    this.fredProvider = fredProvider;
    logger.info("MarketDataService initialized with layered architecture");
  }

  // ===== CORE YIELD CURVE OPERATIONS =====

  /**
   * Get latest yield curve data
   */
  @Cacheable(value = "yieldCurves", key = "'latest'", cacheManager = "yieldCurveCacheManager")
  public YieldCurveResponse getLatestYieldCurve() {
    // Try database first
    Optional<YieldCurveResponse> dbResult = databaseService.getLatestYieldCurve();
    if (dbResult.isPresent()) {
      logger.debug("Retrieved latest yield curve from database");
      return dbResult.get();
    }

    // Try providers
    Optional<YieldCurveResponse> providerResult = providerService.fetchLatestYieldCurve();
    if (providerResult.isPresent()) {
      YieldCurveResponse curve = providerResult.get();
      // Store in database for future use
      databaseService.storeYieldCurve(curve);
      logger.debug("Retrieved latest yield curve from providers and stored in database");
      return curve;
    }

    // Use fallback
    YieldCurveResponse fallbackCurve = fallbackService.createFallbackYieldCurve();
    logger.info("Using fallback yield curve data");
    return fallbackCurve;
  }

  /**
   * Get historical yield curve for a specific date
   */
  @Cacheable(value = "yieldCurves", key = "#date.toString()", cacheManager = "yieldCurveCacheManager")
  public YieldCurveResponse getHistoricalYieldCurve(LocalDate date) {
    // Try database first
    Optional<YieldCurveResponse> dbResult = databaseService.getHistoricalYieldCurve(date);
    if (dbResult.isPresent()) {
      logger.debug("Retrieved historical yield curve for {} from database", date);
      return dbResult.get();
    }

    // Try providers
    Optional<YieldCurveResponse> providerResult = providerService.fetchHistoricalYieldCurve(date);
    if (providerResult.isPresent()) {
      YieldCurveResponse curve = providerResult.get();
      // Store in database for future use
      databaseService.storeYieldCurve(curve);
      logger.debug("Retrieved historical yield curve for {} from providers and stored in database", date);
      return curve;
    }

    // Use fallback
    YieldCurveResponse fallbackCurve = fallbackService.createFallbackYieldCurve(date);
    logger.info("Using fallback yield curve data for date {}", date);
    return fallbackCurve;
  }

  /**
   * Get yield curves for multiple dates (batch operation)
   */
  @Cacheable(value = "yieldCurvesBatch", key = "#dates.toString()")
  public List<YieldCurveResponse> getYieldCurvesForDates(List<LocalDate> dates) {
    // Try database first
    List<YieldCurveResponse> dbResults = databaseService.getYieldCurvesForDates(dates);
    if (!dbResults.isEmpty() && dbResults.size() == dates.size()) {
      logger.debug("Retrieved all {} yield curves from database", dates.size());
      return dbResults;
    }

    // Try providers
    Optional<List<YieldCurveResponse>> providerResults = providerService.fetchYieldCurvesForDates(dates);
    if (providerResults.isPresent()) {
      List<YieldCurveResponse> curves = providerResults.get();
      // Store in database for future use
      curves.forEach(databaseService::storeYieldCurve);
      logger.debug("Retrieved {} yield curves from providers and stored in database", curves.size());
      return curves;
    }

    // Use fallback
    List<YieldCurveResponse> fallbackCurves = fallbackService.createFallbackYieldCurves(dates);
    logger.info("Using fallback yield curve data for {} dates", dates.size());
    return fallbackCurves;
  }

  /**
   * Get time series of yield data for a specific tenor
   */
  @Cacheable(value = "yieldTimeSeries", key = "#tenor + '_' + #startDate + '_' + #endDate")
  public List<MarketData> getYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    // Try database first
    List<MarketData> dbResults = databaseService.getYieldTimeSeries(tenor, startDate, endDate);
    if (!dbResults.isEmpty()) {
      logger.debug("Retrieved time series for {} from database ({} records)", tenor, dbResults.size());
      return dbResults;
    }

    // Try providers
    Optional<List<MarketData>> providerResults = providerService.fetchYieldTimeSeries(tenor, startDate, endDate);
    if (providerResults.isPresent()) {
      List<MarketData> timeSeries = providerResults.get();
      logger.debug("Retrieved time series for {} from providers ({} records)", tenor, timeSeries.size());
      return timeSeries;
    }

    // Return empty list - time series data is less critical
    logger.info("No time series data available for tenor {} from {} to {}", tenor, startDate, endDate);
    return List.of();
  }

  // ===== CREDIT AND SPREAD OPERATIONS =====

  /**
   * Get credit spreads for bond pricing
   */
  @Cacheable(value = "creditSpreads", key = "'current'")
  public Map<String, BigDecimal> getCreditSpreads() {
    // Try database first
    Optional<Map<String, BigDecimal>> dbResult = databaseService.getLatestCreditSpreads();
    if (dbResult.isPresent()) {
      logger.debug("Retrieved credit spreads from database");
      return dbResult.get();
    }

    // Try providers
    Optional<Map<String, BigDecimal>> providerResult = providerService.fetchCreditSpreads();
    if (providerResult.isPresent()) {
      Map<String, BigDecimal> spreads = providerResult.get();
      // Store in database for future use
      databaseService.storeCreditSpreads(spreads, "PROVIDER");
      logger.debug("Retrieved credit spreads from providers and stored in database");
      return spreads;
    }

    // Use fallback
    Map<String, BigDecimal> fallbackSpreads = fallbackService.getFallbackCreditSpreads();
    logger.info("Using fallback credit spread data");
    return fallbackSpreads;
  }

  /**
   * Get inflation expectations for a specific region
   * Flow: Providers -> Fallback (no cache/db for inflation expectations yet)
   */
  @Cacheable(value = "inflationExpectations", key = "#region")
  public Map<String, BigDecimal> getInflationExpectations(String region) {
    // Try providers
    Optional<Map<String, BigDecimal>> providerResult = providerService.fetchInflationExpectations(region);
    if (providerResult.isPresent()) {
      logger.debug("Retrieved inflation expectations for {} from providers", region);
      return providerResult.get();
    }

    // Use fallback
    Map<String, BigDecimal> fallbackExpectations = fallbackService.getFallbackInflationExpectations(region);
    logger.info("Using fallback inflation expectations data for region {}", region);
    return fallbackExpectations;
  }

  /**
   * Get benchmark rates data
   */
  @Cacheable(value = "benchmarkRates", key = "'current'")
  public Map<String, BigDecimal> getBenchmarkRates() {
    // Try database first
    Optional<Map<String, BigDecimal>> dbResult = databaseService.getLatestBenchmarkRates();
    if (dbResult.isPresent()) {
      logger.debug("Retrieved benchmark rates from database");
      return dbResult.get();
    }

    // Try providers
    Optional<Map<String, BigDecimal>> providerResult = providerService.fetchBenchmarkRates();
    if (providerResult.isPresent()) {
      Map<String, BigDecimal> rates = providerResult.get();
      // Store in database for future use
      databaseService.storeBenchmarkRates(rates, "PROVIDER");
      logger.debug("Retrieved benchmark rates from providers and stored in database");
      return rates;
    }

    // Use fallback
    Map<String, BigDecimal> fallbackRates = fallbackService.getFallbackBenchmarkRates();
    logger.info("Using fallback benchmark rate data");
    return fallbackRates;
  }

  /**
   * Get sector-specific credit data
   * Flow: Providers -> Fallback
   */
  @Cacheable(value = "sectorCreditData", key = "#sector")
  public Map<String, BigDecimal> getSectorCreditData(String sector) {
    // Try providers
    Optional<Map<String, BigDecimal>> providerResult = providerService.fetchSectorCreditData(sector);
    if (providerResult.isPresent()) {
      logger.debug("Retrieved sector credit data for {} from providers", sector);
      return providerResult.get();
    }

    // Use fallback
    Map<String, BigDecimal> fallbackData = fallbackService.getFallbackSectorCreditData(sector);
    logger.info("Using fallback credit spreads for sector {}", sector);
    return fallbackData;
  }

  /**
   * Get liquidity premiums by instrument type
   * Flow: Providers -> Fallback
   */
  @Cacheable(value = "liquidityPremiums", key = "'current'")
  public Map<String, BigDecimal> getLiquidityPremiums() {
    // Try providers
    Optional<Map<String, BigDecimal>> providerResult = providerService.fetchLiquidityPremiums();
    if (providerResult.isPresent()) {
      logger.debug("Retrieved liquidity premiums from providers");
      return providerResult.get();
    }

    // Use fallback
    Map<String, BigDecimal> fallbackPremiums = fallbackService.getFallbackLiquidityPremiums();
    logger.info("Using fallback liquidity premium data");
    return fallbackPremiums;
  }

  // ===== CONVENIENCE METHODS FOR PRICING ENGINE =====

  /**
   * Get yield for specific tenor
   */
  public BigDecimal getYieldForTenor(String tenor, String region) {
    // Try database first
    Optional<BigDecimal> dbResult = databaseService.getYieldForTenor(tenor, LocalDate.now());
    if (dbResult.isPresent()) {
      logger.debug("Retrieved yield for tenor {} from database", tenor);
      return dbResult.get();
    }

    // Try getting from latest yield curve (which follows full flow)
    try {
      YieldCurveResponse curve = getLatestYieldCurve();
      BigDecimal yield = curve.getYields().get(tenor);
      if (yield != null) {
        return yield;
      }
    } catch (Exception e) {
      logger.warn("Failed to get yield for tenor {} from curve: {}", tenor, e.getMessage());
    }

    // Fallback to static data
    BigDecimal fallbackYield = fallbackService.getFallbackYieldForTenor(tenor, region);
    logger.debug("Retrieved yield for tenor {} from fallback", tenor);
    return fallbackYield;
  }

  /**
   * Get credit spread for specific rating
   */
  public BigDecimal getCreditSpreadForRating(String rating) {
    // Try database first
    Optional<BigDecimal> dbResult = databaseService.getCreditSpreadForRating(rating);
    if (dbResult.isPresent()) {
      logger.debug("Retrieved credit spread for rating {} from database", rating);
      return dbResult.get();
    }

    // Try getting from credit spreads (which follows full flow)
    try {
      Map<String, BigDecimal> spreads = getCreditSpreads();
      BigDecimal spread = spreads.get(rating.toUpperCase());
      if (spread != null) {
        return spread;
      }
    } catch (Exception e) {
      logger.warn("Failed to get credit spread for rating {}: {}", rating, e.getMessage());
    }

    // Use fallback
    BigDecimal fallbackSpread = fallbackService.getFallbackCreditSpreadForRating(rating);
    logger.debug("Retrieved credit spread for rating {} from fallback", rating);
    return fallbackSpread;
  }

  /**
   * Get benchmark rate for specific region
   */
  public BigDecimal getBenchmarkRateForRegion(String region) {
    // Try database first
    Optional<BigDecimal> dbResult = databaseService.getBenchmarkRateForRegion(region);
    if (dbResult.isPresent()) {
      logger.debug("Retrieved benchmark rate for region {} from database", region);
      return dbResult.get();
    }

    // Try getting from benchmark rates (which follows full flow)
    try {
      Map<String, BigDecimal> rates = getBenchmarkRates();
      BigDecimal rate = rates.get(region.toUpperCase());
      if (rate != null) {
        return rate;
      }
    } catch (Exception e) {
      logger.warn("Failed to get benchmark rate for region {}: {}", region, e.getMessage());
    }

    // Use fallback
    BigDecimal fallbackRate = fallbackService.getFallbackBenchmarkRateForRegion(region);
    logger.debug("Retrieved benchmark rate for region {} from fallback", region);
    return fallbackRate;
  }

  // ===== METADATA AND HEALTH =====

  /**
   * Health check for market data availability
   */
  public boolean isMarketDataAvailable() {
    return providerService.isAnyProviderHealthy();
  }

  /**
   * Get supported tenors from providers
   */
  public Set<String> getSupportedTenors() {
    return fredProvider.getSupportedTenors();
  }

  /**
   * Get provider information for monitoring
   */
  public Map<String, Boolean> getProviderHealthStatus() {
    return providerService.getProviderHealthStatus();
  }

  /**
   * Manual data refresh - clears cache and forces provider fetch
   */
  public void refreshMarketData() {
    logger.info("Manual market data refresh initiated");
    // This would typically involve cache eviction and triggering fresh data fetch
    // Implementation depends on cache configuration
  }

  /**
   * Database maintenance - clean old data
   */
  public void cleanOldData(int daysToKeep) {
    LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
    databaseService.cleanOldData(cutoffDate);
    logger.info("Cleaned market data older than {} days", daysToKeep);
  }
}
