package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.config.FallbackMarketData;
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
 * Centralized market data service that coordinates between different providers
 * and provides fallback data for valuation calculations.
 * This is the main service used internally by the API for fetching any market data.
 */
@Service
public class MarketDataService {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

  private final MarketDataProvider fredProvider;
  private final Optional<MarketDataProvider> alternativeProvider;

  public MarketDataService(
      @Qualifier("fredApiClient") MarketDataProvider fredProvider,
      @Qualifier("alternativeDataClient") Optional<MarketDataProvider> alternativeProvider) {
    this.fredProvider = fredProvider;
    this.alternativeProvider = alternativeProvider;
    logger.info("MarketDataService initialized with FRED provider and {} alternative provider", 
        alternativeProvider.isPresent() ? "enabled" : "disabled");
  }

  // ===== CORE YIELD CURVE OPERATIONS =====

  /**
   * Get latest yield curve data
   */
  @Cacheable(value = "yieldCurves", key = "'latest'", cacheManager = "yieldCurveCacheManager")
  public YieldCurveResponse getLatestYieldCurve() {
    try {
      if (fredProvider.isServiceHealthy()) {
        return fredProvider.fetchLatestYieldCurve();
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed, trying alternative: {}", e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchLatestYieldCurve();
      } catch (Exception e) {
        logger.warn("Alternative provider failed: {}", e.getMessage());
      }
    }

    // Use fallback data
    logger.info("Using fallback yield curve data");
    return createFallbackYieldCurve();
  }

  /**
   * Get historical yield curve for a specific date
   */
  @Cacheable(value = "yieldCurves", key = "#date.toString()", cacheManager = "yieldCurveCacheManager")
  public YieldCurveResponse getHistoricalYieldCurve(LocalDate date) {
    try {
      if (fredProvider.isServiceHealthy()) {
        return fredProvider.fetchHistoricalYieldCurve(date);
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for date {}: {}", date, e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchHistoricalYieldCurve(date);
      } catch (Exception e) {
        logger.warn("Alternative provider failed for date {}: {}", date, e.getMessage());
      }
    }

    // Use fallback data with historical variation
    logger.info("Using fallback yield curve data for date {}", date);
    return createFallbackYieldCurve(date);
  }

  /**
   * Get yield curves for multiple dates (batch operation)
   */
  @Cacheable(value = "yieldCurvesBatch", key = "#dates.toString()")
  public List<YieldCurveResponse> getYieldCurvesForDates(List<LocalDate> dates) {
    try {
      if (fredProvider.isServiceHealthy()) {
        return fredProvider.fetchYieldCurvesForDates(dates);
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for batch dates: {}", e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchYieldCurvesForDates(dates);
      } catch (Exception e) {
        logger.warn("Alternative provider failed for batch dates: {}", e.getMessage());
      }
    }

    // Use fallback data
    logger.info("Using fallback yield curve data for {} dates", dates.size());
    return dates.stream()
      .map(this::createFallbackYieldCurve)
      .toList();
  }

  /**
   * Get time series of yield data for a specific tenor
   */
  @Cacheable(value = "yieldTimeSeries", key = "#tenor + '_' + #startDate + '_' + #endDate")
  public List<MarketData> getYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    try {
      if (fredProvider.isServiceHealthy()) {
        return fredProvider.fetchYieldTimeSeries(tenor, startDate, endDate);
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for time series {}: {}", tenor, e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchYieldTimeSeries(tenor, startDate, endDate);
      } catch (Exception e) {
        logger.warn("Alternative provider failed for time series {}: {}", tenor, e.getMessage());
      }
    }

    // Return empty list for fallback - time series data is less critical
    logger.info("No time series data available for tenor {} from {} to {}", tenor, startDate, endDate);
    return List.of();
  }

  // ===== CREDIT AND SPREAD OPERATIONS =====

  /**
   * Get credit spreads for bond pricing
   */
  @Cacheable(value = "creditSpreads", key = "'current'")
  public Map<String, BigDecimal> getCreditSpreads() {
    // Try alternative provider first
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchCreditSpreads();
      } catch (Exception e) {
        logger.warn("Alternative provider failed for credit spreads: {}", e.getMessage());
      }
    }

    // Use fallback data
    logger.info("Using fallback credit spread data");
    return FallbackMarketData.CREDIT_SPREADS;
  }

  /**
   * Get inflation expectations for a specific region
   */
  @Cacheable(value = "inflationExpectations", key = "#region")
  public Map<String, BigDecimal> getInflationExpectations(String region) {
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchInflationExpectations(region);
      } catch (Exception e) {
        logger.warn("Alternative provider failed for inflation expectations: {}", e.getMessage());
      }
    }

    // Use fallback data
    logger.info("Using fallback inflation expectations data for region {}", region);
    return FallbackMarketData.INFLATION_EXPECTATIONS.getOrDefault(region.toUpperCase(),
        FallbackMarketData.INFLATION_EXPECTATIONS.get("EUR")); // Default to EUR
  }

  /**
   * Get benchmark rates data
   */
  @Cacheable(value = "benchmarkRates", key = "'current'")
  public Map<String, BigDecimal> getBenchmarkRates() {
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchBenchmarkRates();
      } catch (Exception e) {
        logger.warn("Alternative provider failed for benchmark rates: {}", e.getMessage());
      }
    }

    logger.info("Using fallback benchmark rate data");
    return FallbackMarketData.BENCHMARK_RATES;
  }

  /**
   * Get sector-specific credit data
   */
  @Cacheable(value = "sectorCreditData", key = "#sector")
  public Map<String, BigDecimal> getSectorCreditData(String sector) {
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchSectorCreditData(sector);
      } catch (Exception e) {
        logger.warn("Alternative provider failed for sector credit data: {}", e.getMessage());
      }
    }

    // Use fallback data - return general credit spreads for the sector
    logger.info("Using fallback credit spreads for sector {}", sector);
    return FallbackMarketData.CREDIT_SPREADS;
  }

  /**
   * Get liquidity premiums by instrument type
   */
  @Cacheable(value = "liquidityPremiums", key = "'current'")
  public Map<String, BigDecimal> getLiquidityPremiums() {
    if (alternativeProvider.isPresent()) {
      try {
        return alternativeProvider.get().fetchLiquidityPremiums();
      } catch (Exception e) {
        logger.warn("Alternative provider failed for liquidity premiums: {}", e.getMessage());
      }
    }

    logger.info("Using fallback liquidity premium data");
    return FallbackMarketData.LIQUIDITY_PREMIUMS;
  }

  // ===== CONVENIENCE METHODS FOR PRICING ENGINE =====

  /**
   * Get yield for specific tenor
   */
  public BigDecimal getYieldForTenor(String tenor, String region) {
    try {
      YieldCurveResponse curve = getLatestYieldCurve();
      BigDecimal yield = curve.getYields().get(tenor);
      if (yield != null) {
        return yield;
      }
    } catch (Exception e) {
      logger.warn("Failed to get yield for tenor {}: {}", tenor, e.getMessage());
    }

    // Fallback to static data
    Map<String, BigDecimal> regionCurve = FallbackMarketData.YIELD_CURVES.get(region.toUpperCase());
    if (regionCurve == null) {
      regionCurve = FallbackMarketData.YIELD_CURVES.get("EUR"); // Default to EUR
    }
    return regionCurve.get(tenor);
  }

  /**
   * Get credit spread for specific rating
   */
  public BigDecimal getCreditSpreadForRating(String rating) {
    try {
      Map<String, BigDecimal> spreads = getCreditSpreads();
      return spreads.get(rating.toUpperCase());
    } catch (Exception e) {
      logger.warn("Failed to get credit spread for rating {}: {}", rating, e.getMessage());
      return FallbackMarketData.CREDIT_SPREADS.get(rating.toUpperCase());
    }
  }

  /**
   * Get benchmark rate for specific region
   */
  public BigDecimal getBenchmarkRateForRegion(String region) {
    try {
      Map<String, BigDecimal> rates = getBenchmarkRates();
      return rates.get(region.toUpperCase());
    } catch (Exception e) {
      logger.warn("Failed to get benchmark rate for region {}: {}", region, e.getMessage());
      return FallbackMarketData.BENCHMARK_RATES.get(region.toUpperCase());
    }
  }

  // ===== METADATA AND HEALTH =====

  /**
   * Health check for market data availability
   */
  public boolean isMarketDataAvailable() {
    return fredProvider.isServiceHealthy() || 
           (alternativeProvider.isPresent() && alternativeProvider.get().isServiceHealthy());
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
    Map<String, Boolean> status = new java.util.HashMap<>();
    status.put(fredProvider.getProviderName(), fredProvider.isServiceHealthy());
    if (alternativeProvider.isPresent()) {
      MarketDataProvider altProvider = alternativeProvider.get();
      status.put(altProvider.getProviderName(), altProvider.isServiceHealthy());
    }
    return status;
  }

  // ===== PRIVATE HELPER METHODS =====

  private YieldCurveResponse createFallbackYieldCurve() {
    return createFallbackYieldCurve(LocalDate.now());
  }

  private YieldCurveResponse createFallbackYieldCurve(LocalDate date) {
    Map<String, BigDecimal> eurYields = FallbackMarketData.YIELD_CURVES.get("EUR");
    
    return YieldCurveResponse.builder()
      .date(date)
      .source("FALLBACK")
      .yields(eurYields)
      .lastUpdated(LocalDate.now())
      .build();
  }
}
