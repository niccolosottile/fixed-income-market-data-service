package com.fixedincome.marketdata.controller;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;
import com.fixedincome.marketdata.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal testing controller for end-to-end market data flow validation.
 * This controller provides visibility into how data flows through the layered architecture:
 * Cache → Database → Providers → Fallback
 */
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Market Data Testing", description = "Internal testing endpoints for data flow validation")
public class MarketDataTestingController {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataTestingController.class);

  private final MarketDataService marketDataService;

  public MarketDataTestingController(MarketDataService marketDataService) {
    this.marketDataService = marketDataService;
    
    logger.info("MarketDataTestingController initialized for end-to-end testing");
  }

  // ===== DATA FLOW SCENARIO TESTS =====

  @GetMapping("/scenarios/cold-start")
  @Operation(summary = "Test cold start scenario", 
             description = "Clear cache and database, then fetch data 10 times to see full provider flow and caching")
  public ResponseEntity<Map<String, Object>> testColdStart() {
    logger.info("❄️ Testing COLD START scenario - clear cache/database and fetch fresh data 10 times...");
    
    Map<String, Object> results = new HashMap<>();
    results.put("scenario", "Cold Start");
    results.put("expectedFlow", "Call 1: Cache (miss) → Database (miss) → Provider, Calls 2-10: Cache (hit)");
    
    try {
      // Step 1: Clear database (including today's data)
      logger.info("🧹 Step 1: Clearing all database data...");
      marketDataService.clearAllDatabaseData();
      
      // Step 2: Refresh cache (clears it)
      logger.info("🔄 Step 2: Clearing all caches...");
      marketDataService.clearAllCaches();
      
      // Step 3: Fetch data 10 times - first should go through full flow, rest should hit cache
      logger.info("📊 Step 3: Fetching data 10 times - watch logs for flow progression...");
      
      List<Map<String, Object>> calls = new ArrayList<>();
      
      for (int i = 1; i <= 10; i++) {
        logger.info("📞 Call {} - {}...", i, i == 1 ? "should hit provider" : "should hit cache");
        long start = System.currentTimeMillis();
        YieldCurveResponse curve = marketDataService.getLatestYieldCurve();
        long time = System.currentTimeMillis() - start;
        
        calls.add(Map.of(
          "call", i,
          "timeMs", time,
          "source", curve.getSource()
        ));
      }
      
      results.put("calls", calls);
      results.put("message", "✅ First call should be slow (provider), subsequent calls fast (cache)");
      
    } catch (Exception e) {
      logger.error("❌ Cold start test failed: {}", e.getMessage());
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  @GetMapping("/scenarios/warm-cache")
  @Operation(summary = "Test warm cache scenario")
  public ResponseEntity<Map<String, Object>> testWarmCache() {
    logger.info("🔥 Testing WARM CACHE scenario - multiple calls to see cache hits...");
    
    Map<String, Object> results = new HashMap<>();
    results.put("scenario", "Warm Cache");
    results.put("expectedFlow", "Cache (hit) - should be very fast");
    
    try {
      // Make multiple rapid calls
      List<Map<String, Object>> calls = new ArrayList<>();
      
      for (int i = 1; i <= 10; i++) {
        logger.info("📞 Call {} - should hit cache...", i);
        long start = System.currentTimeMillis();
        YieldCurveResponse curve = marketDataService.getLatestYieldCurve();
        long time = System.currentTimeMillis() - start;
        
        calls.add(Map.of(
          "call", i,
          "timeMs", time,
          "source", curve.getSource()
        ));
      }
      
      results.put("calls", calls);
      results.put("message", "✅ All calls should be fast and from same source if cache is working");
      
    } catch (Exception e) {
      logger.error("❌ Warm cache test failed: {}", e.getMessage());
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  // ===== SPECIFIC DATA TYPE TESTS =====

  @GetMapping("/yield-curves/latest")
  @Operation(summary = "Test latest yield curve with source visibility")
  public ResponseEntity<Map<String, Object>> testLatestYieldCurve() {
    logger.info("📈 Testing latest yield curve - watch logs for data source...");
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      YieldCurveResponse curve = marketDataService.getLatestYieldCurve();
      
      results.put("success", true);
      results.put("source", curve.getSource());
      results.put("date", curve.getDate());
      results.put("yields", curve.getYields());
      results.put("lastUpdated", curve.getLastUpdated());
      results.put("message", "✅ Check logs above to see which layer provided the data");
      
    } catch (Exception e) {
      logger.error("❌ Latest yield curve test failed: {}", e.getMessage());
      results.put("success", false);
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  @GetMapping("/yield-curves/historical")
  @Operation(summary = "Test historical yield curve")
  public ResponseEntity<Map<String, Object>> testHistoricalYieldCurve(
    @Parameter(description = "Date for historical data") @RequestParam(defaultValue = "2024-01-15") LocalDate date) {
    
    logger.info("📊 Testing historical yield curve for {} - watch logs for data source...", date);
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      YieldCurveResponse curve = marketDataService.getHistoricalYieldCurve(date);
      
      results.put("success", true);
      results.put("requestedDate", date);
      results.put("source", curve.getSource());
      results.put("actualDate", curve.getDate());
      results.put("yields", curve.getYields());
      results.put("message", "✅ Check logs above to see which layer provided the data");
      
    } catch (Exception e) {
      logger.error("❌ Historical yield curve test failed: {}", e.getMessage());
      results.put("success", false);
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  @GetMapping("/time-series/{tenor}")
  @Operation(summary = "Test time series data")
  public ResponseEntity<Map<String, Object>> testTimeSeries(
    @Parameter(description = "Tenor (e.g., 10Y)") @PathVariable String tenor,
    @Parameter(description = "Start date") @RequestParam(defaultValue = "2024-01-01") LocalDate startDate,
    @Parameter(description = "End date") @RequestParam(defaultValue = "2024-01-31") LocalDate endDate) {
    
    logger.info("📈 Testing time series for {} from {} to {} - watch logs for data source...", 
      tenor, startDate, endDate);
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      List<MarketData> timeSeries = marketDataService.getYieldTimeSeries(tenor, startDate, endDate);
      
      results.put("success", true);
      results.put("tenor", tenor);
      results.put("startDate", startDate);
      results.put("endDate", endDate);
      results.put("dataPoints", timeSeries.size());
      
      if (!timeSeries.isEmpty()) {
        results.put("source", timeSeries.get(0).getSource());
        results.put("sampleData", timeSeries.stream().toList());
      }
      
      results.put("message", "✅ Check logs above to see which layer provided the data");
      
    } catch (Exception e) {
      logger.error("❌ Time series test failed: {}", e.getMessage());
      results.put("success", false);
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  // ===== CACHE TESTING =====

  @PostMapping("/cache/clear")
  @Operation(summary = "Clear all caches", description = "⚠️ CAUTION: Clears all cached data - next requests will hit database/providers")
  public ResponseEntity<Map<String, Object>> clearCaches() {
    logger.warn("⚠️ Clearing all caches for testing purposes...");
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      marketDataService.clearAllCaches();
      
      results.put("success", true);
      results.put("message", "All caches cleared - next API calls will hit database/providers");
      
      logger.info("✅ All caches cleared for testing");
      
    } catch (Exception e) {
      logger.error("❌ Cache clear failed: {}", e.getMessage());
      results.put("success", false);
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  @PostMapping("/database/clear")
  @Operation(summary = "Clear database for testing", description = "⚠️ CAUTION: Removes all market data from database")
  public ResponseEntity<Map<String, Object>> clearDatabase() {
    logger.warn("⚠️ Clearing database for testing purposes...");
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      // Clear all database data
      marketDataService.clearAllDatabaseData();
      
      results.put("success", true);
      results.put("message", "Database cleared - next API calls will hit providers");
      
      logger.info("✅ Database cleared for testing");
      
    } catch (Exception e) {
      logger.error("❌ Database clear failed: {}", e.getMessage());
      results.put("success", false);
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  @GetMapping("/supported-tenors")
  @Operation(summary = "Get supported tenors")
  public ResponseEntity<Map<String, Object>> getSupportedTenors() {
    logger.info("📋 Retrieving supported tenors...");
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      Set<String> tenors = marketDataService.getSupportedTenors();
      results.put("supportedTenors", tenors);
      results.put("count", tenors.size());
      
    } catch (Exception e) {
      logger.error("❌ Failed to get supported tenors: {}", e.getMessage());
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }

  @GetMapping("/market-data-status")
  @Operation(summary = "Overall market data service status")
  public ResponseEntity<Map<String, Object>> getMarketDataStatus() {
    logger.info("🔍 Checking overall market data service status...");
    
    Map<String, Object> results = new HashMap<>();
    
    try {
      results.put("serviceAvailable", marketDataService.isMarketDataAvailable());
      results.put("providerHealth", marketDataService.getProviderHealthStatus());
      results.put("supportedTenors", marketDataService.getSupportedTenors());
      results.put("timestamp", LocalDate.now());
      
    } catch (Exception e) {
      logger.error("❌ Failed to get market data status: {}", e.getMessage());
      results.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(results);
  }
}