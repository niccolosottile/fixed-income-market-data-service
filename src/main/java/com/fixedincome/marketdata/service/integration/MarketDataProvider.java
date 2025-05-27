package com.fixedincome.marketdata.service.integration;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common interface for all market data providers.
 * Any client (FRED, Alternative, Bloomberg, etc.) can implement any subset of these methods
 * based on what data they provide.
 */
public interface MarketDataProvider {
  
  // ===== CORE YIELD CURVE OPERATIONS =====
  
  /**
   * Fetches the latest yield curve data
   * @return YieldCurveResponse with latest yield curve data
   */
  YieldCurveResponse fetchLatestYieldCurve();
  
  /**
   * Fetches historical yield curve data for a specific date
   * @param date The date to fetch data for
   * @return YieldCurveResponse with historical yield curve data
   */
  YieldCurveResponse fetchHistoricalYieldCurve(LocalDate date);
  
  /**
   * Fetches yield curve data for multiple dates
   * @param dates List of dates to fetch data for
   * @return List of YieldCurveResponse objects
   */
  List<YieldCurveResponse> fetchYieldCurvesForDates(List<LocalDate> dates);
  
  /**
   * Fetches a time series of yield data for a specific tenor
   * @param tenor The tenor (e.g., "10Y")
   * @param startDate Beginning of the time period
   * @param endDate End of the time period
   * @return List of yield data points
   */
  List<MarketData> fetchYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate);
  
  // ===== CREDIT AND SPREAD OPERATIONS =====
  
  /**
   * Fetches credit spreads by rating
   * @return Map of credit rating to spread value in basis points
   * @throws UnsupportedOperationException if provider doesn't support this data
   */
  Map<String, BigDecimal> fetchCreditSpreads();
  
  /**
   * Fetches inflation expectations data (breakeven rates)
   * @param region Region code (e.g., "US", "EUR", "UK")
   * @return Map of terms to breakeven inflation rates
   * @throws UnsupportedOperationException if provider doesn't support this data
   */
  Map<String, BigDecimal> fetchInflationExpectations(String region);
  
  /**
   * Fetches benchmark rates for different regions (central bank rates)
   * @return Map of region code to benchmark rate
   * @throws UnsupportedOperationException if provider doesn't support this data
   */
  Map<String, BigDecimal> fetchBenchmarkRates();
  
  /**
   * Fetches sector-specific credit data for corporate bonds
   * @param sector Industry sector (e.g., "TECH", "FINANCE", "ENERGY")
   * @return Map of ratings to sector-specific credit spreads
   * @throws UnsupportedOperationException if provider doesn't support this data
   */
  Map<String, BigDecimal> fetchSectorCreditData(String sector);
  
  /**
   * Fetches liquidity premium data by instrument type
   * @return Map of instrument types to liquidity premium values
   * @throws UnsupportedOperationException if provider doesn't support this data
   */
  Map<String, BigDecimal> fetchLiquidityPremiums();
  
  // ===== METADATA AND HEALTH =====
  
  /**
   * Health check for the data source
   * @return true if the service is available and responding
   */
  boolean isServiceHealthy();
  
  /**
   * Get the name/identifier of this provider
   * @return Provider name (e.g., "FRED", "Alternative", "Bloomberg")
   */
  String getProviderName();
  
  /**
   * Get the set of tenors supported by this provider
   * @return Set of supported tenor strings
   */
  Set<String> getSupportedTenors();
}
