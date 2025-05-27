package com.fixedincome.marketdata.service.integration;

import com.fixedincome.marketdata.config.FredApiProperties;
import com.fixedincome.marketdata.dto.FredApiResponse;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.exception.MarketDataException;
import com.fixedincome.marketdata.model.MarketData;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FredApiClient extends AbstractMarketDataProvider {
  
  // FRED API Series IDs for Euro Area Government Bond yields
  private static final Map<String, String> YIELD_CURVE_SERIES;
  
  static {
    Map<String, String> series = new HashMap<>();
    // Euro Area Government Bond yields from ECB
    series.put("1M", "IRLTLT01EZM156N");
    series.put("3M", "IRLTLT01EZQ156N"); 
    series.put("6M", "IR3TIB01EZM156N");
    series.put("1Y", "IRLTLT01EZA156N");
    series.put("2Y", "IRLTLT02EZA156N");
    series.put("3Y", "IRLTLT03EZA156N");
    series.put("5Y", "IRLTLT05EZA156N");
    series.put("7Y", "IRLTLT07EZA156N");
    series.put("10Y", "IRLTLT10EZA156N");
    series.put("20Y", "IRLTLT20EZA156N");
    series.put("30Y", "IRLTLT30EZA156N");
    YIELD_CURVE_SERIES = Collections.unmodifiableMap(series);
  }
  
  private static final String SOURCE = "FRED";
  
  private final RestTemplate restTemplate;
  private final FredApiProperties fredApiProperties;
  
  public FredApiClient(RestTemplate restTemplate, FredApiProperties fredApiProperties) {
    this.restTemplate = restTemplate;
    this.fredApiProperties = fredApiProperties;
  }
  
  // ===== ABSTRACT METHOD IMPLEMENTATIONS =====
  
  @Override
  protected void validateApiConfiguration() {
    if (fredApiProperties.getApiKey() == null || fredApiProperties.getApiKey().trim().isEmpty()) {
      throw new MarketDataException("FRED API key is not configured");
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
  
  // ===== CORE YIELD CURVE OPERATIONS =====
  
  /**
   * Fetches the latest yield curve data from FRED
   * @return YieldCurveResponse with latest yield curve data
   */
  @Cacheable(value = "yieldCurves", key = "'latest'")
  @Override
  public YieldCurveResponse fetchLatestYieldCurve() {
    logger.info("Fetching latest yield curve data from FRED");
    
    validateApiConfiguration();
    
    Map<String, BigDecimal> yieldCurve = new ConcurrentHashMap<>();
    
    // Fetch data for each tenor concurrently
    List<CompletableFuture<Void>> futures = YIELD_CURVE_SERIES.entrySet().stream()
      .map(entry -> CompletableFuture.runAsync(() -> {
        try {
          String tenor = entry.getKey();
          String seriesId = entry.getValue();
          BigDecimal yield = fetchLatestYieldForSeries(seriesId);
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
      throw new MarketDataException("No yield curve data could be fetched from FRED");
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
  @Cacheable(value = "yieldCurves", key = "#date.toString()")
  @Override
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
          BigDecimal yield = fetchYieldForSeriesOnDate(seriesId, date);
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
  @Cacheable(value = "yieldTimeSeries", key = "#tenor + '_' + #startDate + '_' + #endDate")
  @Override
  public List<MarketData> fetchYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    logger.info("Fetching yield time series for tenor {} from {} to {}", tenor, startDate, endDate);
    
    validateApiConfiguration();
    validateTenor(tenor);
    validateDateRange(startDate, endDate);
    
    String seriesId = YIELD_CURVE_SERIES.get(tenor);
    
    try {
      FredApiResponse response = fetchFredData(seriesId, startDate, endDate);
      List<MarketData> marketDataList = convertToMarketDataList(response, tenor);
      
      if (marketDataList.isEmpty()) {
        logger.warn("No data found for tenor {} in date range {} to {}", tenor, startDate, endDate);
      }
      
      logger.info("Successfully fetched {} yield data points for tenor {}", 
        marketDataList.size(), tenor);
      return marketDataList;
        
    } catch (RestClientException e) {
      throw new MarketDataException("Failed to fetch yield time series from FRED API for tenor: " + tenor, e);
    }
  }
  
  /**
   * Fetches yield curve data for multiple dates
   * @param dates List of dates to fetch data for
   * @return List of YieldCurveResponse objects
   */
  @Cacheable(value = "yieldCurvesBatch", key = "#dates.toString()")
  @Override
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
  
  /**
   * Health check for the FRED API service
   * @return true if the service is available and responding
   */
  @Override
  public boolean isServiceHealthy() {
    try {
      // Use existing validation method
      validateApiConfiguration();
      
      // Test the API with a simple request for the 10Y yield (most commonly available)
      String testSeriesId = YIELD_CURVE_SERIES.get("10Y");
      if (testSeriesId == null) {
        logger.warn("Test series ID not found for health check");
        return false;
      }
      
      // Make a minimal request to check API availability
      String url = buildFredUrl(testSeriesId, null, null, 1, "desc");
      FredApiResponse response = restTemplate.getForObject(url, FredApiResponse.class);
      
      // Check if we got a valid response
      if (response != null && response.getObservations() != null) {
        logger.debug("FRED API health check passed");
        return true;
      } else {
        logger.warn("FRED API health check failed: received invalid response");
        return false;
      }
      
    } catch (MarketDataException e) {
      // This will catch validation errors from validateApiConfiguration()
      logger.warn("FRED API health check failed: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      logger.warn("FRED API health check failed: {}", e.getMessage());
      return false;
    }
  }
  
  // ===== PRIVATE HELPER METHODS =====
  
  private BigDecimal fetchLatestYieldForSeries(String seriesId) {
    try {
      String url = buildFredUrl(seriesId, null, null, 1, "desc");
      FredApiResponse response = restTemplate.getForObject(url, FredApiResponse.class);
      
      if (response != null && response.getObservations() != null && !response.getObservations().isEmpty()) {
        FredApiResponse.FredObservation latestObs = response.getObservations().get(0);
        return parseYieldValue(latestObs.getValue());
      }
      
      return null;
    } catch (Exception e) {
      logger.error("Error fetching latest yield for series {}: {}", seriesId, e.getMessage());
      throw new MarketDataException("Failed to fetch latest yield for series: " + seriesId, e);
    }
  }
  
  private BigDecimal fetchYieldForSeriesOnDate(String seriesId, LocalDate date) {
    try {
      String url = buildFredUrl(seriesId, date, date, 1, "desc");
      FredApiResponse response = restTemplate.getForObject(url, FredApiResponse.class);
      
      if (response != null && response.getObservations() != null && !response.getObservations().isEmpty()) {
        FredApiResponse.FredObservation obs = response.getObservations().get(0);
        return parseYieldValue(obs.getValue());
      }
      
      return null;
    } catch (Exception e) {
      logger.error("Error fetching yield for series {} on date {}: {}", seriesId, date, e.getMessage());
      throw new MarketDataException("Failed to fetch yield for series " + seriesId + " on date " + date, e);
    }
  }
  
  private FredApiResponse fetchFredData(String seriesId, LocalDate startDate, LocalDate endDate) {
    String url = buildFredUrl(seriesId, startDate, endDate, null, "asc");
    
    try {
      FredApiResponse response = restTemplate.getForObject(url, FredApiResponse.class);
      if (response == null) {
        throw new MarketDataException("Received null response from FRED API for series: " + seriesId);
      }
      return response;
    } catch (RestClientException e) {
      logger.error("Error calling FRED API for series {}: {}", seriesId, e.getMessage());
      throw new MarketDataException("Failed to fetch data from FRED API for series: " + seriesId, e);
    }
  }
  
  private String buildFredUrl(String seriesId, LocalDate startDate, LocalDate endDate, 
                              Integer limit, String sortOrder) {
    UriComponentsBuilder builder = UriComponentsBuilder
      .fromUriString(fredApiProperties.getBaseUrl())
      .queryParam("series_id", seriesId)
      .queryParam("api_key", fredApiProperties.getApiKey())
      .queryParam("file_type", "json");
        
    if (startDate != null) {
      builder.queryParam("observation_start", startDate.format(DATE_FORMATTER));
    }
    if (endDate != null) {
      builder.queryParam("observation_end", endDate.format(DATE_FORMATTER));
    }
    if (limit != null) {
      builder.queryParam("limit", limit);
    }
    if (sortOrder != null) {
      builder.queryParam("sort_order", sortOrder);
    }
    
    return builder.build().toUriString();
  }
  
  private List<MarketData> convertToMarketDataList(FredApiResponse response, String tenor) {
    if (response.getObservations() == null) {
      return Collections.emptyList();
    }
    
    return response.getObservations().stream()
      .filter(obs -> obs.getValue() != null && !".".equals(obs.getValue()))
      .map(obs -> {
        BigDecimal yieldValue = parseYieldValue(obs.getValue());
        if (yieldValue != null) {
          return MarketData.builder()
            .dataType(MarketData.DataType.YIELD_CURVE)
            .dataKey(tenor)
            .dataValue(yieldValue)
            .dataDate(LocalDate.parse(obs.getDate(), DATE_FORMATTER))
            .source(SOURCE)
            .currency("EUR")
            .tenor(tenor)
            .build();
        }
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
}
