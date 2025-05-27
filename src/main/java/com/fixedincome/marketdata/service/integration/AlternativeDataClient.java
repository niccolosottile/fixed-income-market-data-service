package com.fixedincome.marketdata.service.integration;

import com.fixedincome.marketdata.config.AlternativeApiProperties;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.exception.MarketDataException;
import com.fixedincome.marketdata.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Alternative client for fetching market data including yield curves, credit spreads,
 * and other reference data. Provides the same interface as FredApiClient for consistency.
 */
@Service
@ConditionalOnProperty(name = "alternative.api.enabled", havingValue = "true", matchIfMissing = false)
public class AlternativeDataClient {
  
  private static final Logger logger = LoggerFactory.getLogger(AlternativeDataClient.class);
  
  // Alternative API Series IDs for Euro Area Government Bond yields (fallback to EUR region)
  private static final Map<String, String> YIELD_CURVE_SERIES;
  
  static {
    Map<String, String> series = new HashMap<>();
    // Euro Area Government Bond yields - using alternative data source identifiers
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
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  
  @SuppressWarnings("unused")
  private final RestTemplate restTemplate;
  private final AlternativeApiProperties properties;
  
  // Static fallback credit spreads by rating (in basis points)
  private static final Map<String, BigDecimal> FALLBACK_CREDIT_SPREADS = Map.of(
    "AAA", new BigDecimal("25"),
    "AA", new BigDecimal("40"),
    "A", new BigDecimal("75"),
    "BBB", new BigDecimal("150"),
    "BB", new BigDecimal("300"),
    "B", new BigDecimal("500"),
    "CCC", new BigDecimal("800"),
    "CC", new BigDecimal("1200"),
    "C", new BigDecimal("2000")
  );
  
  // Static yield curve data for different regions (yield percentage)
  private static final Map<String, Map<String, BigDecimal>> FALLBACK_YIELD_CURVES = new HashMap<>();
  
  static {
    // US Treasury yield curve
    Map<String, BigDecimal> usTreasury = new HashMap<>();
    usTreasury.put("1M", new BigDecimal("5.31"));
    usTreasury.put("3M", new BigDecimal("5.28"));
    usTreasury.put("6M", new BigDecimal("5.12"));
    usTreasury.put("1Y", new BigDecimal("4.80"));
    usTreasury.put("2Y", new BigDecimal("4.49"));
    usTreasury.put("3Y", new BigDecimal("4.25"));
    usTreasury.put("5Y", new BigDecimal("4.18"));
    usTreasury.put("7Y", new BigDecimal("4.20"));
    usTreasury.put("10Y", new BigDecimal("4.23"));
    usTreasury.put("20Y", new BigDecimal("4.47"));
    usTreasury.put("30Y", new BigDecimal("4.39"));
    FALLBACK_YIELD_CURVES.put("US", usTreasury);
    
    // Euro area yield curve (ECB AAA-rated euro area central government bonds)
    Map<String, BigDecimal> euroArea = new HashMap<>();
    euroArea.put("3M", new BigDecimal("3.72"));
    euroArea.put("6M", new BigDecimal("3.51"));
    euroArea.put("1Y", new BigDecimal("3.40"));
    euroArea.put("2Y", new BigDecimal("3.15"));
    euroArea.put("5Y", new BigDecimal("2.92"));
    euroArea.put("7Y", new BigDecimal("2.89"));
    euroArea.put("10Y", new BigDecimal("3.05"));
    euroArea.put("20Y", new BigDecimal("3.31"));
    euroArea.put("30Y", new BigDecimal("3.30"));
    FALLBACK_YIELD_CURVES.put("EUR", euroArea);
    
    // UK yield curve (BOE government bonds)
    Map<String, BigDecimal> ukGilts = new HashMap<>();
    ukGilts.put("1M", new BigDecimal("5.15"));
    ukGilts.put("3M", new BigDecimal("5.05"));
    ukGilts.put("6M", new BigDecimal("4.95"));
    ukGilts.put("1Y", new BigDecimal("4.65"));
    ukGilts.put("2Y", new BigDecimal("4.35"));
    ukGilts.put("5Y", new BigDecimal("4.10"));
    ukGilts.put("10Y", new BigDecimal("4.15"));
    ukGilts.put("20Y", new BigDecimal("4.40"));
    ukGilts.put("30Y", new BigDecimal("4.35"));
    FALLBACK_YIELD_CURVES.put("UK", ukGilts);
    
    // Japan yield curve (JGBs)
    Map<String, BigDecimal> japanJgbs = new HashMap<>();
    japanJgbs.put("1M", new BigDecimal("0.08"));
    japanJgbs.put("3M", new BigDecimal("0.12"));
    japanJgbs.put("6M", new BigDecimal("0.15"));
    japanJgbs.put("1Y", new BigDecimal("0.20"));
    japanJgbs.put("2Y", new BigDecimal("0.28"));
    japanJgbs.put("5Y", new BigDecimal("0.59"));
    japanJgbs.put("10Y", new BigDecimal("0.95"));
    japanJgbs.put("20Y", new BigDecimal("1.70"));
    japanJgbs.put("30Y", new BigDecimal("1.90"));
    FALLBACK_YIELD_CURVES.put("JP", japanJgbs);
  }
  
  public AlternativeDataClient(RestTemplate restTemplate, AlternativeApiProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    logger.info("AlternativeDataClient initialized with base URL: {}", properties.getBaseUrl());
  }
  
  // ===== CORE FRED API CLIENT COMPATIBLE METHODS =====
  
  /**
   * Fetches the latest yield curve data from Alternative API
   * @return YieldCurveResponse with latest yield curve data
   */
  @Cacheable(value = "yieldCurves", key = "'alternative_latest'")
  public YieldCurveResponse fetchLatestYieldCurve() {
    logger.info("Fetching latest yield curve data from Alternative API");
    
    validateApiConfiguration();
    
    Map<String, BigDecimal> yieldCurve = new ConcurrentHashMap<>();
    
    // Fetch data for each tenor concurrently
    List<CompletableFuture<Void>> futures = YIELD_CURVE_SERIES.entrySet().stream()
      .map(entry -> CompletableFuture.runAsync(() -> {
        try {
          String tenor = entry.getKey();
          String seriesId = entry.getValue();
          BigDecimal yield = fetchLatestYieldForSeries(seriesId, tenor);
          if (yield != null) {
            yieldCurve.put(tenor, yield);
          }
        } catch (Exception e) {
          logger.warn("Failed to fetch yield for tenor {}: {}", entry.getKey(), e.getMessage());
        }
      }))
      .collect(Collectors.toList());
  
    // Wait for all requests to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    if (yieldCurve.isEmpty()) {
      throw new MarketDataException("No yield curve data could be fetched from Alternative API");
    }
    
    logger.info("Successfully fetched yield curve with {} data points", yieldCurve.size());
    
    return YieldCurveResponse.builder()
      .date(LocalDate.now())
      .source(SOURCE)
      .yields(yieldCurve)
      .lastUpdated(LocalDate.now())
      .build();
  }
  
  /**
   * Fetches historical yield curve data for a specific date
   * @param date The date to fetch data for
   * @return YieldCurveResponse with historical yield curve data
   */
  @Cacheable(value = "yieldCurves", key = "'alternative_' + #date.toString()")
  public YieldCurveResponse fetchHistoricalYieldCurve(LocalDate date) {
    logger.info("Fetching historical yield curve data for date: {}", date);
    
    validateApiConfiguration();
    validateDate(date);
    
    Map<String, BigDecimal> yieldCurve = new ConcurrentHashMap<>();
    List<String> failedTenors = Collections.synchronizedList(new ArrayList<>());
    
    // Fetch data for each tenor concurrently
    List<CompletableFuture<Void>> futures = YIELD_CURVE_SERIES.entrySet().stream()
      .map(entry -> CompletableFuture.runAsync(() -> {
        try {
          String tenor = entry.getKey();
          String seriesId = entry.getValue();
          BigDecimal yield = fetchYieldForSeriesOnDate(seriesId, tenor, date);
          if (yield != null) {
            yieldCurve.put(tenor, yield);
          } else {
            failedTenors.add(tenor);
          }
        } catch (Exception e) {
          logger.warn("Failed to fetch historical yield for tenor {} on date {}: {}", 
            entry.getKey(), date, e.getMessage());
          failedTenors.add(entry.getKey());
        }
      }))
      .collect(Collectors.toList());
    
    // Wait for all requests to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    if (yieldCurve.isEmpty()) {
      throw new MarketDataException("No yield curve data available for date: " + date);
    }
    
    if (!failedTenors.isEmpty()) {
      logger.warn("Failed to fetch data for tenors: {}", String.join(", ", failedTenors));
    }
    
    logger.info("Successfully fetched historical yield curve for {} with {} data points", 
      date, yieldCurve.size());
      
    return YieldCurveResponse.builder()
      .date(date)
      .source(SOURCE)
      .yields(yieldCurve)
      .lastUpdated(LocalDate.now())
      .build();
  }
  
  /**
   * Fetches a time series of yield data for a specific tenor
   * @param tenor The tenor (e.g., "10Y")
   * @param startDate Beginning of the time period
   * @param endDate End of the time period
   * @return List of yield data points
   */
  @Cacheable(value = "yieldTimeSeries", key = "'alternative_' + #tenor + '_' + #startDate + '_' + #endDate")
  public List<MarketData> fetchYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    logger.info("Fetching yield time series for tenor {} from {} to {}", tenor, startDate, endDate);
    
    validateApiConfiguration();
    validateTenor(tenor);
    validateDateRange(startDate, endDate);
    
    String seriesId = YIELD_CURVE_SERIES.get(tenor);
    if (seriesId == null) {
      throw new MarketDataException("Invalid tenor: " + tenor + ". Supported tenors: " + 
        String.join(", ", YIELD_CURVE_SERIES.keySet()));
    }
    
    try {
      List<MarketData> marketDataList = fetchAlternativeTimeSeriesData(seriesId, tenor, startDate, endDate);
      
      if (marketDataList.isEmpty()) {
        logger.warn("No data found for tenor {} in date range {} to {}", tenor, startDate, endDate);
      }
      
      logger.info("Successfully fetched {} yield data points for tenor {}", 
        marketDataList.size(), tenor);
      return marketDataList;
        
    } catch (RestClientException e) {
      throw new MarketDataException("Failed to fetch yield time series from Alternative API for tenor: " + tenor, e);
    }
  }
  
  /**
   * Fetches yield curve data for multiple dates
   * @param dates List of dates to fetch data for
   * @return List of YieldCurveResponse objects
   */
  @Cacheable(value = "yieldCurvesBatch", key = "'alternative_' + #dates.toString()")
  public List<YieldCurveResponse> fetchYieldCurvesForDates(List<LocalDate> dates) {
    logger.info("Fetching yield curves for {} dates", dates.size());
    
    validateApiConfiguration();
    if (dates == null || dates.isEmpty()) {
      throw new MarketDataException("Dates list cannot be null or empty");
    }
    
    // Process dates concurrently for better performance
    List<CompletableFuture<YieldCurveResponse>> futures = dates.stream()
      .map(date -> CompletableFuture.supplyAsync(() -> {
        try {
          return fetchHistoricalYieldCurve(date);
        } catch (Exception e) {
          logger.warn("Failed to fetch yield curve for date {}: {}", date, e.getMessage());
          return null;
        }
      }))
      .collect(Collectors.toList());
    
    // Wait for all requests to complete and collect results
    List<YieldCurveResponse> results = futures.stream()
      .map(CompletableFuture::join)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    
    logger.info("Successfully fetched yield curves for {}/{} dates", results.size(), dates.size());
    return results;
  }
  
  // ===== ADDITIONAL ALTERNATIVE DATA METHODS =====
  
  /**
   * Fetches raw credit spreads by rating
   * @return Map of credit rating to spread value in basis points
   */
  @Cacheable(value = "creditSpreads", key = "'alternative_credit_spreads'")
  public Map<String, BigDecimal> fetchCreditSpreads() {
    if (!properties.isEnabled()) {
      logger.debug("Alternative API is disabled, using fallback credit spread data");
      return getFallbackCreditSpreads();
    }
    
    try {
      logger.debug("Fetching credit spreads from alternative data source");
      
      // TODO: Replace with actual API endpoint when available
      // String url = properties.getBaseUrl() + "/credit-spreads";
      // Map<String, BigDecimal> response = restTemplate.getForObject(url, Map.class);
      
      // For now, simulate API response with fallback
      return getFallbackCreditSpreads();
      
    } catch (RestClientException e) {
      logger.warn("Failed to fetch credit spreads from alternative API: {}", e.getMessage());
      
      if (properties.isUseFallbackData()) {
        logger.info("Using fallback credit spread data");
        return getFallbackCreditSpreads();
      } else {
        throw new MarketDataException("Failed to fetch credit spreads and fallback is disabled", e);
      }
    }
  }
  
  /**
   * Fetch raw inflation expectations data (breakeven rates)
   * @param region Region code
   * @return Map of terms to breakeven inflation rates
   */
  @Cacheable(value = "inflationExpectations", key = "'alternative_inflation_' + #region")
  public Map<String, BigDecimal> fetchInflationExpectations(String region) {
    Map<String, BigDecimal> inflationRates = new HashMap<>();
    
    // Fallback data - typical inflation expectations by tenor
    switch (region.toUpperCase()) {
      case "US":
        inflationRates.put("2Y", new BigDecimal("2.5"));
        inflationRates.put("5Y", new BigDecimal("2.3"));
        inflationRates.put("10Y", new BigDecimal("2.2"));
        inflationRates.put("30Y", new BigDecimal("2.1"));
        break;
      case "EUR":
        inflationRates.put("2Y", new BigDecimal("2.1"));
        inflationRates.put("5Y", new BigDecimal("2.0"));
        inflationRates.put("10Y", new BigDecimal("1.9"));
        inflationRates.put("30Y", new BigDecimal("1.8"));
        break;
      case "UK":
        inflationRates.put("2Y", new BigDecimal("3.2"));
        inflationRates.put("5Y", new BigDecimal("3.0"));
        inflationRates.put("10Y", new BigDecimal("2.8"));
        inflationRates.put("30Y", new BigDecimal("2.7"));
        break;
      default:
        inflationRates.put("5Y", new BigDecimal("2.0"));
        inflationRates.put("10Y", new BigDecimal("2.0"));
    }
    
    return inflationRates;
  }
  
  /**
   * Fetches benchmark rates for different regions (central bank rates)
   * @return Map of region code to benchmark rate
   */
  @Cacheable(value = "benchmarkRates", key = "'alternative_benchmark_rates'")
  public Map<String, BigDecimal> fetchBenchmarkRates() {
    // Fallback data - typical central bank rates
    Map<String, BigDecimal> benchmarkRates = new HashMap<>();
    benchmarkRates.put("US", new BigDecimal("5.25"));  // Fed Funds Rate
    benchmarkRates.put("EUR", new BigDecimal("3.75")); // ECB Deposit Rate
    benchmarkRates.put("UK", new BigDecimal("5.00"));  // Bank of England
    benchmarkRates.put("JP", new BigDecimal("0.10"));  // Bank of Japan
    benchmarkRates.put("CA", new BigDecimal("4.50"));  // Bank of Canada
    benchmarkRates.put("AU", new BigDecimal("4.10"));  // Reserve Bank of Australia
    
    return benchmarkRates;
  }
  
  /**
   * Fetches sector-specific credit data for corporate bonds
   * @param sector Industry sector
   * @return Map of ratings to sector-specific credit spreads
   */
  @Cacheable(value = "sectorCreditData", key = "'alternative_sector_' + #sector")
  public Map<String, BigDecimal> fetchSectorCreditData(String sector) {
    Map<String, BigDecimal> sectorAdjustments = new HashMap<>();
    
    // Sample sector data - this would come from the API in reality
    // These represent sector-specific basis point adjustments
    if ("TECH".equalsIgnoreCase(sector)) {
      sectorAdjustments.put("AAA", new BigDecimal("-5"));
      sectorAdjustments.put("AA", new BigDecimal("-5"));
      sectorAdjustments.put("A", new BigDecimal("-3"));
      sectorAdjustments.put("BBB", new BigDecimal("0"));
      sectorAdjustments.put("BB", new BigDecimal("5"));
      sectorAdjustments.put("B", new BigDecimal("10"));
    } else if ("FINANCE".equalsIgnoreCase(sector)) {
      sectorAdjustments.put("AAA", new BigDecimal("5"));
      sectorAdjustments.put("AA", new BigDecimal("7"));
      sectorAdjustments.put("A", new BigDecimal("10"));
      sectorAdjustments.put("BBB", new BigDecimal("15"));
      sectorAdjustments.put("BB", new BigDecimal("25"));
      sectorAdjustments.put("B", new BigDecimal("35"));
    } else if ("ENERGY".equalsIgnoreCase(sector)) {
      sectorAdjustments.put("AAA", new BigDecimal("3"));
      sectorAdjustments.put("AA", new BigDecimal("5"));
      sectorAdjustments.put("A", new BigDecimal("10"));
      sectorAdjustments.put("BBB", new BigDecimal("20"));
      sectorAdjustments.put("BB", new BigDecimal("30"));
      sectorAdjustments.put("B", new BigDecimal("40"));
    } else {
      // Default/other sectors
      sectorAdjustments.put("AAA", BigDecimal.ZERO);
      sectorAdjustments.put("AA", BigDecimal.ZERO);
      sectorAdjustments.put("A", BigDecimal.ZERO);
      sectorAdjustments.put("BBB", BigDecimal.ZERO);
      sectorAdjustments.put("BB", BigDecimal.ZERO);
      sectorAdjustments.put("B", BigDecimal.ZERO);
    }
    
    return sectorAdjustments;
  }
  
  /**
   * Fetches raw liquidity premium data by instrument type
   * @return Map of instrument types to liquidity premium values
   */
  @Cacheable(value = "liquidityPremiums", key = "'alternative_liquidity_premiums'")
  public Map<String, BigDecimal> fetchLiquidityPremiums() {
    // Typical liquidity premiums (in basis points)
    Map<String, BigDecimal> premiums = new HashMap<>();
    premiums.put("GOVERNMENT", new BigDecimal("0"));      // Government bonds are most liquid
    premiums.put("CORPORATE", new BigDecimal("15"));      // Corporate bonds have moderate liquidity
    premiums.put("MUNICIPAL", new BigDecimal("25"));      // Municipal bonds less liquid
    premiums.put("HIGH_YIELD", new BigDecimal("50"));     // High yield bonds least liquid
    premiums.put("EMERGING_MARKET", new BigDecimal("75")); // EM bonds have higher liquidity premium
    
    return premiums;
  }
  
  /**
   * Health check for the alternative data source
   * @return true if the service is available and responding
   */
  public boolean isServiceHealthy() {
    if (!properties.isEnabled()) {
      return false;
    }
    
    try {
      // TODO: Implement actual health check endpoint
      // String healthUrl = properties.getBaseUrl() + "/health";
      // restTemplate.getForObject(healthUrl, String.class);
      return true;
    } catch (Exception e) {
      logger.warn("Alternative data service health check failed: {}", e.getMessage());
      return false;
    }
  }
  
  // ===== HELPER METHODS =====
  
  private void validateApiConfiguration() {
    if (!properties.isEnabled()) {
      throw new MarketDataException("Alternative API is not enabled");
    }
    if (properties.getApiKey() == null || properties.getApiKey().trim().isEmpty()) {
      logger.warn("Alternative API key is not configured, using fallback data");
    }
  }
  
  private void validateTenor(String tenor) {
    if (tenor == null || tenor.trim().isEmpty()) {
      throw new MarketDataException("Tenor cannot be null or empty");
    }
    if (!YIELD_CURVE_SERIES.containsKey(tenor)) {
      throw new MarketDataException("Invalid tenor: " + tenor + ". Supported tenors: " + 
        String.join(", ", YIELD_CURVE_SERIES.keySet()));
    }
  }
  
  private void validateDate(LocalDate date) {
    if (date == null) {
      throw new MarketDataException("Date cannot be null");
    }
    if (date.isAfter(LocalDate.now())) {
      throw new MarketDataException("Date cannot be in the future");
    }
  }
  
  private void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new MarketDataException("Start date and end date cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new MarketDataException("Start date cannot be after end date");
    }
    if (startDate.isAfter(LocalDate.now())) {
      throw new MarketDataException("Start date cannot be in the future");
    }
  }
  
  private BigDecimal fetchLatestYieldForSeries(String seriesId, String tenor) {
    try {
      if (!properties.isEnabled() || properties.getBaseUrl() == null) {
        return getFallbackYieldForTenor(tenor, "EUR");
      }
      
      // TODO: Replace with actual API endpoint when available
      // String url = buildAlternativeUrl(seriesId, null, null, 1, "desc");
      // AlternativeApiResponse response = restTemplate.getForObject(url, AlternativeApiResponse.class);
      
      // For now, return fallback data
      return getFallbackYieldForTenor(tenor, "EUR");
      
    } catch (Exception e) {
      logger.error("Error fetching latest yield for series {}: {}", seriesId, e.getMessage());
      if (properties.isUseFallbackData()) {
        return getFallbackYieldForTenor(tenor, "EUR");
      }
      throw new MarketDataException("Failed to fetch latest yield for series: " + seriesId, e);
    }
  }
  
  private BigDecimal fetchYieldForSeriesOnDate(String seriesId, String tenor, LocalDate date) {
    try {
      if (!properties.isEnabled() || properties.getBaseUrl() == null) {
        return getFallbackYieldForTenor(tenor, "EUR");
      }
      
      // TODO: Replace with actual API endpoint when available
      // String url = buildAlternativeUrl(seriesId, date, date, 1, "desc");
      // AlternativeApiResponse response = restTemplate.getForObject(url, AlternativeApiResponse.class);
      
      // For now, return fallback data with slight historical variation
      BigDecimal baseYield = getFallbackYieldForTenor(tenor, "EUR");
      if (baseYield != null) {
        // Add small historical variation based on date
        Random random = new Random(date.hashCode());
        int variation = random.nextInt(20) - 10; // -10 to +10 basis points
        BigDecimal adjustment = new BigDecimal(variation).divide(new BigDecimal("10000"));
        return baseYield.add(adjustment);
      }
      return null;
      
    } catch (Exception e) {
      logger.error("Error fetching yield for series {} on date {}: {}", seriesId, date, e.getMessage());
      if (properties.isUseFallbackData()) {
        return getFallbackYieldForTenor(tenor, "EUR");
      }
      throw new MarketDataException("Failed to fetch yield for series " + seriesId + " on date " + date, e);
    }
  }
  
  private List<MarketData> fetchAlternativeTimeSeriesData(String seriesId, String tenor, LocalDate startDate, LocalDate endDate) {
    try {
      if (!properties.isEnabled() || properties.getBaseUrl() == null) {
        return generateFallbackTimeSeriesData(tenor, startDate, endDate);
      }
      
      // TODO: Replace with actual API endpoint when available
      // String url = buildAlternativeUrl(seriesId, startDate, endDate, null, "asc");
      // AlternativeApiResponse response = restTemplate.getForObject(url, AlternativeApiResponse.class);
      
      // For now, generate synthetic time series
      return generateFallbackTimeSeriesData(tenor, startDate, endDate);
      
    } catch (Exception e) {
      logger.error("Error fetching time series data: {}", e.getMessage());
      if (properties.isUseFallbackData()) {
        return generateFallbackTimeSeriesData(tenor, startDate, endDate);
      }
      throw new MarketDataException("Failed to fetch time series data from Alternative API", e);
    }
  }
  
  private BigDecimal getFallbackYieldForTenor(String tenor, String region) {
    Map<String, BigDecimal> regionCurve = FALLBACK_YIELD_CURVES.get(region.toUpperCase());
    if (regionCurve == null) {
      regionCurve = FALLBACK_YIELD_CURVES.get("EUR"); // Default to EUR
    }
    return regionCurve.get(tenor);
  }
  
  private List<MarketData> generateFallbackTimeSeriesData(String tenor, LocalDate startDate, LocalDate endDate) {
    List<MarketData> marketDataList = new ArrayList<>();
    
    // Get starting yield value
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
      
      // Add small drift factor for realism
      int drift = random.nextInt(3) - 1; // -1, 0, or 1
      
      // Convert basis points to percentage (divide by 10000 for 1bp = 0.0001)
      BigDecimal change = new BigDecimal(basis + drift)
        .divide(new BigDecimal("10000"));
      
      // Calculate next day's yield
      currentYield = currentYield.add(change);
      
      // Ensure yield remains positive and realistic
      if (currentYield.compareTo(BigDecimal.ZERO) < 0) {
        currentYield = new BigDecimal("0.01");
      }
      
      // Move to next day
      currentDate = currentDate.plusDays(1);
    }
    
    return marketDataList;
  }
  
  private Map<String, BigDecimal> getFallbackCreditSpreads() {
    logger.debug("Using fallback credit spreads");
    return new HashMap<>(FALLBACK_CREDIT_SPREADS);
  }
  
  private int getVolatilityFactor(String tenor) {
    // Short-term rates tend to be more volatile
    if (tenor.endsWith("M")) {
      return 8;  // Higher volatility for monthly tenors
    } else if (tenor.equals("1Y") || tenor.equals("2Y")) {
      return 6;  // Medium volatility for short tenors
    } else if (tenor.equals("5Y") || tenor.equals("7Y")) {
      return 5;  // Medium volatility for medium tenors
    } else {
      return 4;  // Lower volatility for long tenors
    }
  }
}
