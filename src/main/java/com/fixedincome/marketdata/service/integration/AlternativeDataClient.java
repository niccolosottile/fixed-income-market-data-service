package com.fixedincome.marketdata.service.integration;

import com.fixedincome.marketdata.config.AlternativeApiProperties;
import com.fixedincome.marketdata.config.FallbackMarketData;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.exception.MarketDataException;
import com.fixedincome.marketdata.model.MarketData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Alternative client for fetching market data.
 */
@Service("alternativeDataClient")
@ConditionalOnProperty(name = "alternative.api.enabled", havingValue = "true", matchIfMissing = false)
public class AlternativeDataClient extends AbstractMarketDataProvider {

  // Alternative API Series IDs for Euro Area Government Bond yields
  private static final Map<String, String> YIELD_CURVE_SERIES;

  static {
    Map<String, String> series = new HashMap<>();
    series.put("1M", "ALT_EUR_1M");
    series.put("3M", "ALT_EUR_3M"); 
    series.put("6M", "ALT_EUR_6M");
    series.put("1Y", "ALT_EUR_1Y");
    series.put("2Y", "ALT_EUR_2Y");
    series.put("3Y", "ALT_EUR_3Y");
    series.put("5Y", "ALT_EUR_5Y");
    series.put("7Y", "ALT_EUR_7Y");
    series.put("10Y", "ALT_EUR_10Y");
    series.put("20Y", "ALT_EUR_20Y");
    series.put("30Y", "ALT_EUR_30Y");
    YIELD_CURVE_SERIES = Collections.unmodifiableMap(series);
  }

  private static final String SOURCE = "ALTERNATIVE_API";

  @SuppressWarnings("unused")
  private final RestTemplate restTemplate;
  private final AlternativeApiProperties properties;

  public AlternativeDataClient(RestTemplate restTemplate, AlternativeApiProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    logger.info("AlternativeDataClient initialized with base URL: {}", properties.getBaseUrl());
  }

  // ===== ABSTRACT METHOD IMPLEMENTATIONS =====

  @Override
  protected void validateApiConfiguration() {
    if (!properties.isEnabled()) {
      throw new MarketDataException("Alternative API is not enabled");
    }
    if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
      logger.warn("Alternative API key is not configured, using fallback data");
    }
  }

  @Override
  protected Map<String, String> getYieldCurveSeriesMapping() {
    return YIELD_CURVE_SERIES;
  }

  @Override
  public String getProviderName() {
    return SOURCE;
  }

  /**
   * Fetches the latest yield curve data from Alternative API
   */
  @Override
  public YieldCurveResponse fetchLatestYieldCurve() {
    logger.info("Fetching latest yield curve data from Alternative API");

    validateApiConfiguration();

    if (!properties.isEnabled() || properties.getBaseUrl() == null) {
      logger.info("Alternative API not configured, using fallback data");
      return createFallbackYieldCurve();
    }

    // TODO: Replace with actual API call when available
    // For now, return fallback data
    return createFallbackYieldCurve();
  }

  /**
   * Fetches historical yield curve data for a specific date
   */
  @Override
  public YieldCurveResponse fetchHistoricalYieldCurve(LocalDate date) {
    logger.info("Fetching historical yield curve data for date: {}", date);

    validateApiConfiguration();
    validateDate(date);

    if (!properties.isEnabled() || properties.getBaseUrl() == null) {
      return createFallbackYieldCurve(date);
    }

    // TODO: Replace with actual API call when available
    return createFallbackYieldCurve(date);
  }

  /**
   * Fetches yield time series data
   */
  @Override
  public List<MarketData> fetchYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    logger.info("Fetching yield time series for tenor {} from {} to {}", tenor, startDate, endDate);

    validateApiConfiguration();
    validateTenor(tenor);
    validateDateRange(startDate, endDate);

    if (!properties.isEnabled() || properties.getBaseUrl() == null) {
      return generateFallbackTimeSeriesData(tenor, startDate, endDate);
    }

    // TODO: Replace with actual API call when available
    return generateFallbackTimeSeriesData(tenor, startDate, endDate);
  }

  /**
   * Fetches multiple yield curves for batch processing
   */
  @Override
  public List<YieldCurveResponse> fetchYieldCurvesForDates(List<LocalDate> dates) {
    logger.info("Fetching yield curves for {} dates", dates.size());

    validateApiConfiguration();
    if (dates == null || dates.isEmpty()) {
      throw new MarketDataException("Dates list cannot be null or empty");
    }

    return dates.stream()
      .map(this::fetchHistoricalYieldCurve)
      .collect(Collectors.toList());
  }

  /**
   * Fetches credit spreads by rating
   */
  @Override
  public Map<String, BigDecimal> fetchCreditSpreads() {
    if (!properties.isEnabled()) {
      logger.debug("Alternative API is disabled, using fallback credit spread data");
      return new HashMap<>(FallbackMarketData.CREDIT_SPREADS);
    }

    try {
      logger.debug("Fetching credit spreads from alternative data source");

      // TODO: Replace with actual API endpoint when available
      // For now, simulate API response with fallback
      return new HashMap<>(FallbackMarketData.CREDIT_SPREADS);

    } catch (RestClientException e) {
      logger.warn("Failed to fetch credit spreads from alternative API: {}", e.getMessage());

      if (properties.isUseFallbackData()) {
        logger.info("Using fallback credit spread data");
        return new HashMap<>(FallbackMarketData.CREDIT_SPREADS);
      } else {
        throw new MarketDataException("Failed to fetch credit spreads and fallback is disabled", e);
      }
    }
  }

  /**
   * Fetches benchmark rates for different regions
   */
  @Override
  public Map<String, BigDecimal> fetchBenchmarkRates() {
    return new HashMap<>(FallbackMarketData.BENCHMARK_RATES);
  }

  /**
   * Health check for the alternative data source
   */
  @Override
  public boolean isServiceHealthy() {
    if (!properties.isEnabled()) {
      return false;
    }

    try {
      // TODO: Implement actual health check endpoint
      return true;
    } catch (Exception e) {
      logger.warn("Alternative data service health check failed: {}", e.getMessage());
      return false;
    }
  }

  // ===== PRIVATE HELPER METHODS =====

  private YieldCurveResponse createFallbackYieldCurve() {
    return createFallbackYieldCurve(LocalDate.now());
  }

  private YieldCurveResponse createFallbackYieldCurve(LocalDate date) {
    Map<String, BigDecimal> eurYields = FallbackMarketData.YIELD_CURVES.get("EUR");
    
    return YieldCurveResponse.builder()
      .date(date)
      .source(SOURCE)
      .yields(eurYields)
      .lastUpdated(LocalDate.now())
      .build();
  }

  private List<MarketData> generateFallbackTimeSeriesData(String tenor, LocalDate startDate, LocalDate endDate) {
    List<MarketData> marketDataList = new ArrayList<>();

    // Get starting yield value from centralized fallback data
    BigDecimal currentYield = getFallbackYieldForTenor(tenor, "EUR");
    if (currentYield == null) {
      currentYield = new BigDecimal("3.0"); // Default fallback
    }

    Random random = new Random(tenor.hashCode());
    LocalDate currentDate = startDate;

    // Generate data points from start to end date
    while (!currentDate.isAfter(endDate)) {
      MarketData marketData = MarketData.builder()
        .dataType(MarketData.DataType.YIELD_CURVE)
        .dataKey(tenor)
        .dataValue(currentYield)
        .dataDate(currentDate)
        .source(SOURCE)
        .currency("EUR")
        .tenor(tenor)
        .build();

      marketDataList.add(marketData);

      // Generate small random changes for next day (realistic volatility)
      int volatilityFactor = getVolatilityFactor(tenor);
      int basis = random.nextInt(volatilityFactor) - (volatilityFactor / 2);
      int drift = random.nextInt(3) - 1; // -1, 0, or 1

      // Convert basis points to percentage
      BigDecimal change = new BigDecimal(basis + drift).divide(new BigDecimal("10000"));
      currentYield = currentYield.add(change);

      // Ensure yield remains positive and realistic
      if (currentYield.compareTo(BigDecimal.ZERO) < 0) {
        currentYield = new BigDecimal("0.01");
      }

      currentDate = currentDate.plusDays(1);
    }

    return marketDataList;
  }

  private BigDecimal getFallbackYieldForTenor(String tenor, String region) {
    Map<String, BigDecimal> regionCurve = FallbackMarketData.YIELD_CURVES.get(region.toUpperCase());
    if (regionCurve == null) {
      regionCurve = FallbackMarketData.YIELD_CURVES.get("EUR"); // Default to EUR
    }
    return regionCurve.get(tenor);
  }
}
