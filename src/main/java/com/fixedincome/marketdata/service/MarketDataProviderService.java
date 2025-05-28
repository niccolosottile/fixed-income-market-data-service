package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;
import com.fixedincome.marketdata.service.integration.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service that orchestrates calls to different market data providers.
 * Handles the provider fallback logic (FRED -> Alternative -> None).
 */
@Service
public class MarketDataProviderService {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataProviderService.class);
  
  private final MarketDataProvider fredProvider;
  private final Optional<MarketDataProvider> alternativeProvider;

  // Constructor for Spring dependency injection
  public MarketDataProviderService(
    @Qualifier("fredApiClient") MarketDataProvider fredProvider,
    @Qualifier("alternativeDataClient") Optional<MarketDataProvider> alternativeProvider) {
    this.fredProvider = fredProvider;
    this.alternativeProvider = alternativeProvider;
  }

  // ===== YIELD CURVE PROVIDER OPERATIONS =====

  /**
   * Get latest yield curve from providers
   */
  public Optional<YieldCurveResponse> fetchLatestYieldCurve() {
    // Try FRED first
    try {
      if (fredProvider.isServiceHealthy()) {
        YieldCurveResponse result = fredProvider.fetchLatestYieldCurve();
        if (result != null) {
          logger.debug("Successfully fetched latest yield curve from FRED");
          return Optional.of(result);
        }
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for latest yield curve: {}", e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        YieldCurveResponse result = alternativeProvider.get().fetchLatestYieldCurve();
        if (result != null) {
          logger.debug("Successfully fetched latest yield curve from alternative provider");
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for latest yield curve: {}", e.getMessage());
      }
    }

    logger.info("No providers available for latest yield curve");
    return Optional.empty();
  }

  /**
   * Get historical yield curve from providers
   */
  public Optional<YieldCurveResponse> fetchHistoricalYieldCurve(LocalDate date) {
    // Try FRED first
    try {
      if (fredProvider.isServiceHealthy()) {
        YieldCurveResponse result = fredProvider.fetchHistoricalYieldCurve(date);
        if (result != null) {
          logger.debug("Successfully fetched historical yield curve from FRED for date {}", date);
          return Optional.of(result);
        }
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for historical yield curve on {}: {}", date, e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        YieldCurveResponse result = alternativeProvider.get().fetchHistoricalYieldCurve(date);
        if (result != null) {
          logger.debug("Successfully fetched historical yield curve from alternative provider for date {}", date);
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for historical yield curve on {}: {}", date, e.getMessage());
      }
    }

    logger.info("No providers available for historical yield curve on {}", date);
    return Optional.empty();
  }

  /**
   * Get yield curves for multiple dates from providers
   */
  public Optional<List<YieldCurveResponse>> fetchYieldCurvesForDates(List<LocalDate> dates) {
    // Try FRED first
    try {
      if (fredProvider.isServiceHealthy()) {
        List<YieldCurveResponse> result = fredProvider.fetchYieldCurvesForDates(dates);
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched {} yield curves from FRED", result.size());
          return Optional.of(result);
        }
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for batch yield curves: {}", e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        List<YieldCurveResponse> result = alternativeProvider.get().fetchYieldCurvesForDates(dates);
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched {} yield curves from alternative provider", result.size());
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for batch yield curves: {}", e.getMessage());
      }
    }

    logger.info("No providers available for batch yield curves");
    return Optional.empty();
  }

  /**
   * Get time series data from providers
   */
  public Optional<List<MarketData>> fetchYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    // Try FRED first
    try {
      if (fredProvider.isServiceHealthy()) {
        List<MarketData> result = fredProvider.fetchYieldTimeSeries(tenor, startDate, endDate);
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched time series for {} from FRED", tenor);
          return Optional.of(result);
        }
      }
    } catch (Exception e) {
      logger.warn("FRED provider failed for time series {}: {}", tenor, e.getMessage());
    }

    // Try alternative provider
    if (alternativeProvider.isPresent()) {
      try {
        List<MarketData> result = alternativeProvider.get().fetchYieldTimeSeries(tenor, startDate, endDate);
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched time series for {} from alternative provider", tenor);
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for time series {}: {}", tenor, e.getMessage());
      }
    }

    logger.info("No providers available for time series {}", tenor);
    return Optional.empty();
  }

  // ===== CREDIT AND SPREAD PROVIDER OPERATIONS =====

  /**
   * Get credit spreads from providers
   */
  public Optional<Map<String, BigDecimal>> fetchCreditSpreads() {
    // Try alternative provider first (as per original logic)
    if (alternativeProvider.isPresent()) {
      try {
        Map<String, BigDecimal> result = alternativeProvider.get().fetchCreditSpreads();
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched credit spreads from alternative provider");
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for credit spreads: {}", e.getMessage());
      }
    }

    logger.info("No providers available for credit spreads");
    return Optional.empty();
  }

  /**
   * Get inflation expectations from providers
   */
  public Optional<Map<String, BigDecimal>> fetchInflationExpectations(String region) {
    if (alternativeProvider.isPresent()) {
      try {
        Map<String, BigDecimal> result = alternativeProvider.get().fetchInflationExpectations(region);
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched inflation expectations for {} from alternative provider", region);
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for inflation expectations: {}", e.getMessage());
      }
    }

    logger.info("No providers available for inflation expectations for region {}", region);
    return Optional.empty();
  }

  /**
   * Get benchmark rates from providers
   */
  public Optional<Map<String, BigDecimal>> fetchBenchmarkRates() {
    if (alternativeProvider.isPresent()) {
      try {
        Map<String, BigDecimal> result = alternativeProvider.get().fetchBenchmarkRates();
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched benchmark rates from alternative provider");
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for benchmark rates: {}", e.getMessage());
      }
    }

    logger.info("No providers available for benchmark rates");
    return Optional.empty();
  }

  /**
   * Get sector credit data from providers
   */
  public Optional<Map<String, BigDecimal>> fetchSectorCreditData(String sector) {
    if (alternativeProvider.isPresent()) {
      try {
        Map<String, BigDecimal> result = alternativeProvider.get().fetchSectorCreditData(sector);
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched sector credit data for {} from alternative provider", sector);
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for sector credit data: {}", e.getMessage());
      }
    }

    logger.info("No providers available for sector credit data for sector {}", sector);
    return Optional.empty();
  }

  /**
   * Get liquidity premiums from providers
   */
  public Optional<Map<String, BigDecimal>> fetchLiquidityPremiums() {
    if (alternativeProvider.isPresent()) {
      try {
        Map<String, BigDecimal> result = alternativeProvider.get().fetchLiquidityPremiums();
        if (result != null && !result.isEmpty()) {
          logger.debug("Successfully fetched liquidity premiums from alternative provider");
          return Optional.of(result);
        }
      } catch (Exception e) {
        logger.warn("Alternative provider failed for liquidity premiums: {}", e.getMessage());
      }
    }

    logger.info("No providers available for liquidity premiums");
    return Optional.empty();
  }

  // ===== UTILITY METHODS =====

  /**
   * Check if any provider is available
   */
  public boolean isAnyProviderHealthy() {
    try {
      boolean fredHealthy = fredProvider.isServiceHealthy();
      boolean altHealthy = alternativeProvider.isPresent() && alternativeProvider.get().isServiceHealthy();
      return fredHealthy || altHealthy;
    } catch (Exception e) {
      logger.warn("Error checking provider health: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Get provider health status
   */
  public Map<String, Boolean> getProviderHealthStatus() {
    Map<String, Boolean> status = new java.util.HashMap<>();
    
    try {
      status.put(fredProvider.getProviderName(), fredProvider.isServiceHealthy());
    } catch (Exception e) {
      status.put(fredProvider.getProviderName(), false);
    }
    
    if (alternativeProvider.isPresent()) {
      MarketDataProvider altProvider = alternativeProvider.get();
      try {
        status.put(altProvider.getProviderName(), altProvider.isServiceHealthy());
      } catch (Exception e) {
        status.put(altProvider.getProviderName(), false);
      }
    }
    
    return status;
  }

  /**
   * Get supported tenors from primary provider
   */
  public Set<String> getSupportedTenors() {
    return fredProvider.getSupportedTenors();
  }
}