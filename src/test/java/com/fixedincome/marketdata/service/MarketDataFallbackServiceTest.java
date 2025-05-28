package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.config.FallbackMarketData;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataFallbackService Tests")
class MarketDataFallbackServiceTest {

  private MarketDataFallbackService fallbackService;

  @BeforeEach
  void setUp() {
    fallbackService = new MarketDataFallbackService();
  }

  @Nested
  @DisplayName("Yield Curve Fallback Operations")
  class YieldCurveFallbackOperationsTest {

    @Test
    @DisplayName("Should return fallback yield curve for EUR region")
    void testGetFallbackYieldCurve_EurRegion() {
      // When
      YieldCurveResponse result = fallbackService.getFallbackYieldCurve("EUR");

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
      assertEquals(LocalDate.now(), result.getDate());
      assertEquals(LocalDate.now(), result.getLastUpdated());
      
      Map<String, BigDecimal> yields = result.getYields();
      assertNotNull(yields);
      assertTrue(yields.containsKey("1Y"));
      assertTrue(yields.containsKey("5Y"));
      assertTrue(yields.containsKey("10Y"));
      assertTrue(yields.containsKey("30Y"));
      
      // Verify EUR specific rates
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), yields.get("10Y"));
    }

    @Test
    @DisplayName("Should return fallback yield curve for US region")
    void testGetFallbackYieldCurve_UsRegion() {
      // When
      YieldCurveResponse result = fallbackService.getFallbackYieldCurve("US");

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
      
      Map<String, BigDecimal> yields = result.getYields();
      // Verify US specific rates
      assertEquals(FallbackMarketData.YIELD_CURVES.get("US").get("10Y"), yields.get("10Y"));
    }

    @Test
    @DisplayName("Should default to EUR when region not found")
    void testGetFallbackYieldCurve_UnknownRegion() {
      // When
      YieldCurveResponse result = fallbackService.getFallbackYieldCurve("UNKNOWN");

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
      
      Map<String, BigDecimal> yields = result.getYields();
      // Should default to EUR rates
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), yields.get("10Y"));
    }

    @Test
    @DisplayName("Should handle null region gracefully")
    void testGetFallbackYieldCurve_NullRegion() {
      // When
      YieldCurveResponse result = fallbackService.getFallbackYieldCurve(null);

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
      
      Map<String, BigDecimal> yields = result.getYields();
      // Should default to EUR rates
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), yields.get("10Y"));
    }

    @Test
    @DisplayName("Should return fallback yield curves for multiple dates")
    void testGetFallbackYieldCurvesForDates_Success() {
      // Given
      List<LocalDate> dates = List.of(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2),
        LocalDate.of(2024, 1, 3)
      );

      // When
      List<YieldCurveResponse> result = fallbackService.getFallbackYieldCurvesForDates(dates, "EUR");

      // Then
      assertNotNull(result);
      assertEquals(3, result.size());
      
      for (int i = 0; i < result.size(); i++) {
        YieldCurveResponse response = result.get(i);
        assertEquals("FALLBACK", response.getSource());
        assertEquals(dates.get(i), response.getDate());
        assertEquals(LocalDate.now(), response.getLastUpdated());
        assertTrue(response.getYields().containsKey("10Y"));
      }
    }

    @Test
    @DisplayName("Should handle empty dates list")
    void testGetFallbackYieldCurvesForDates_EmptyList() {
      // When
      List<YieldCurveResponse> result = fallbackService.getFallbackYieldCurvesForDates(List.of(), "EUR");

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null dates list")
    void testGetFallbackYieldCurvesForDates_NullList() {
      // When
      List<YieldCurveResponse> result = fallbackService.getFallbackYieldCurvesForDates(null, "EUR");

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Credit Spreads Fallback Operations")
  class CreditSpreadsFallbackOperationsTest {

    @Test
    @DisplayName("Should return fallback credit spreads")
    void testGetFallbackCreditSpreads_Success() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackCreditSpreads();

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Verify it contains expected ratings
      assertTrue(result.containsKey("AAA"));
      assertTrue(result.containsKey("AA"));
      assertTrue(result.containsKey("A"));
      assertTrue(result.containsKey("BBB"));
      assertTrue(result.containsKey("BB"));
      assertTrue(result.containsKey("B"));
      
      // Verify values match fallback data
      assertEquals(FallbackMarketData.CREDIT_SPREADS.get("AAA"), result.get("AAA"));
      assertEquals(FallbackMarketData.CREDIT_SPREADS.get("BBB"), result.get("BBB"));
    }

    @Test
    @DisplayName("Should return sector specific credit data")
    void testGetFallbackSectorCreditData_TechSector() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackSectorCreditData("TECH");

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Tech sector should have slightly lower spreads
      assertTrue(result.containsKey("BBB"));
      assertTrue(result.get("BBB").compareTo(FallbackMarketData.CREDIT_SPREADS.get("BBB")) < 0);
    }

    @Test
    @DisplayName("Should return sector specific credit data for energy")
    void testGetFallbackSectorCreditData_EnergySector() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackSectorCreditData("ENERGY");

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Energy sector should have higher spreads
      assertTrue(result.containsKey("BBB"));
      assertTrue(result.get("BBB").compareTo(FallbackMarketData.CREDIT_SPREADS.get("BBB")) > 0);
    }

    @Test
    @DisplayName("Should return default credit spreads for unknown sector")
    void testGetFallbackSectorCreditData_UnknownSector() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackSectorCreditData("UNKNOWN");

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Should return base credit spreads
      assertEquals(FallbackMarketData.CREDIT_SPREADS.get("BBB"), result.get("BBB"));
    }

    @Test
    @DisplayName("Should handle null sector gracefully")
    void testGetFallbackSectorCreditData_NullSector() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackSectorCreditData(null);

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Should return base credit spreads
      assertEquals(FallbackMarketData.CREDIT_SPREADS.get("BBB"), result.get("BBB"));
    }
  }

  @Nested
  @DisplayName("Benchmark Rates Fallback Operations")
  class BenchmarkRatesFallbackOperationsTest {

    @Test
    @DisplayName("Should return fallback benchmark rates")
    void testGetFallbackBenchmarkRates_Success() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackBenchmarkRates();

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Verify it contains expected regions
      assertTrue(result.containsKey("US"));
      assertTrue(result.containsKey("EUR"));
      assertTrue(result.containsKey("UK"));
      assertTrue(result.containsKey("JP"));
      
      // Verify values match fallback data
      assertEquals(FallbackMarketData.BENCHMARK_RATES.get("US"), result.get("US"));
      assertEquals(FallbackMarketData.BENCHMARK_RATES.get("EUR"), result.get("EUR"));
    }
  }

  @Nested
  @DisplayName("Inflation Expectations Fallback Operations")
  class InflationExpectationsFallbackOperationsTest {

    @Test
    @DisplayName("Should return fallback inflation expectations for US")
    void testGetFallbackInflationExpectations_UsRegion() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackInflationExpectations("US");

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Should contain various tenors
      assertTrue(result.containsKey("5Y"));
      assertTrue(result.containsKey("10Y"));
      
      // US should have target around 2%
      assertTrue(result.get("10Y").compareTo(new BigDecimal("1.5")) > 0);
      assertTrue(result.get("10Y").compareTo(new BigDecimal("3.0")) < 0);
    }

    @Test
    @DisplayName("Should return fallback inflation expectations for EUR")
    void testGetFallbackInflationExpectations_EurRegion() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackInflationExpectations("EUR");

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      assertTrue(result.containsKey("10Y"));
      // EUR should have target around 2%
      assertTrue(result.get("10Y").compareTo(new BigDecimal("1.5")) > 0);
      assertTrue(result.get("10Y").compareTo(new BigDecimal("3.0")) < 0);
    }

    @Test
    @DisplayName("Should return default inflation expectations for unknown region")
    void testGetFallbackInflationExpectations_UnknownRegion() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackInflationExpectations("UNKNOWN");

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Should return reasonable default values
      assertTrue(result.containsKey("10Y"));
      assertTrue(result.get("10Y").compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should handle null region gracefully")
    void testGetFallbackInflationExpectations_NullRegion() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackInflationExpectations(null);

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      assertTrue(result.containsKey("10Y"));
    }
  }

  @Nested
  @DisplayName("Liquidity Premiums Fallback Operations")
  class LiquidityPremiumsFallbackOperationsTest {

    @Test
    @DisplayName("Should return fallback liquidity premiums")
    void testGetFallbackLiquidityPremiums_Success() {
      // When
      Map<String, BigDecimal> result = fallbackService.getFallbackLiquidityPremiums();

      // Then
      assertNotNull(result);
      assertFalse(result.isEmpty());
      
      // Should contain different instrument types
      assertTrue(result.containsKey("GOVERNMENT"));
      assertTrue(result.containsKey("CORPORATE"));
      assertTrue(result.containsKey("MUNICIPAL"));
      
      // Government should have lowest premiums
      assertTrue(result.get("GOVERNMENT").compareTo(result.get("CORPORATE")) < 0);
      
      // All premiums should be positive
      result.values().forEach(premium -> 
        assertTrue(premium.compareTo(BigDecimal.ZERO) >= 0));
    }
  }

  @Nested
  @DisplayName("Utility Methods")
  class UtilityMethodsTest {

    @Test
    @DisplayName("Should get yield for specific tenor and region")
    void testGetYieldForTenor_Success() {
      // Given
      String tenor = "10Y";
      String region = "EUR";

      // When
      BigDecimal result = fallbackService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), result);
    }

    @Test
    @DisplayName("Should get yield for tenor with unknown region")
    void testGetYieldForTenor_UnknownRegion() {
      // Given
      String tenor = "10Y";
      String region = "UNKNOWN";

      // When
      BigDecimal result = fallbackService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      // Should default to EUR
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), result);
    }

    @Test
    @DisplayName("Should get yield for unknown tenor")
    void testGetYieldForTenor_UnknownTenor() {
      // Given
      String tenor = "100Y";
      String region = "EUR";

      // When
      BigDecimal result = fallbackService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      // Should return a reasonable default (likely 30Y rate)
      assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should get credit spread for specific rating")
    void testGetCreditSpreadForRating_Success() {
      // Given
      String rating = "BBB";

      // When
      BigDecimal result = fallbackService.getCreditSpreadForRating(rating);

      // Then
      assertNotNull(result);
      assertEquals(FallbackMarketData.CREDIT_SPREADS.get("BBB"), result);
    }

    @Test
    @DisplayName("Should get credit spread for unknown rating")
    void testGetCreditSpreadForRating_UnknownRating() {
      // Given
      String rating = "UNKNOWN";

      // When
      BigDecimal result = fallbackService.getCreditSpreadForRating(rating);

      // Then
      assertNotNull(result);
      // Should return a reasonable default
      assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should get benchmark rate for specific region")
    void testGetBenchmarkRateForRegion_Success() {
      // Given
      String region = "US";

      // When
      BigDecimal result = fallbackService.getBenchmarkRateForRegion(region);

      // Then
      assertNotNull(result);
      assertEquals(FallbackMarketData.BENCHMARK_RATES.get("US"), result);
    }

    @Test
    @DisplayName("Should get benchmark rate for unknown region")
    void testGetBenchmarkRateForRegion_UnknownRegion() {
      // Given
      String region = "UNKNOWN";

      // When
      BigDecimal result = fallbackService.getBenchmarkRateForRegion(region);

      // Then
      assertNotNull(result);
      // Should return a reasonable default
      assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputsHandling() {
      // Test all methods with null inputs
      assertNotNull(fallbackService.getYieldForTenor(null, null));
      assertNotNull(fallbackService.getCreditSpreadForRating(null));
      assertNotNull(fallbackService.getBenchmarkRateForRegion(null));
      
      assertNotNull(fallbackService.getFallbackYieldCurve(null));
      assertNotNull(fallbackService.getFallbackInflationExpectations(null));
      assertNotNull(fallbackService.getFallbackSectorCreditData(null));
    }
  }

  @Nested
  @DisplayName("Fallback Data Consistency")
  class FallbackDataConsistencyTest {

    @Test
    @DisplayName("Should have consistent yield curve data across regions")
    void testYieldCurveConsistency() {
      // Verify all regions have the same tenors
      Map<String, Map<String, BigDecimal>> yieldCurves = FallbackMarketData.YIELD_CURVES;
      
      assertFalse(yieldCurves.isEmpty());
      
      // Get tenor set from first region
      String firstRegion = yieldCurves.keySet().iterator().next();
      var expectedTenors = yieldCurves.get(firstRegion).keySet();
      
      // Verify all other regions have the same tenors
      yieldCurves.values().forEach(regionData -> 
        assertEquals(expectedTenors, regionData.keySet()));
    }

    @Test
    @DisplayName("Should have reasonable yield curve ordering")
    void testYieldCurveOrdering() {
      // Test that longer tenors generally have higher yields (normal yield curve)
      Map<String, BigDecimal> eurCurve = FallbackMarketData.YIELD_CURVES.get("EUR");
      
      if (eurCurve.containsKey("1Y") && eurCurve.containsKey("30Y")) {
        // 30Y should typically be higher than 1Y
        assertTrue(eurCurve.get("30Y").compareTo(eurCurve.get("1Y")) > 0,
          "30Y yield should be higher than 1Y yield in normal conditions");
      }
    }

    @Test
    @DisplayName("Should have reasonable credit spread ordering")
    void testCreditSpreadOrdering() {
      // Test that lower ratings have higher spreads
      Map<String, BigDecimal> spreads = FallbackMarketData.CREDIT_SPREADS;
      
      if (spreads.containsKey("AAA") && spreads.containsKey("BBB")) {
        assertTrue(spreads.get("BBB").compareTo(spreads.get("AAA")) > 0,
          "BBB spread should be higher than AAA spread");
      }
      
      if (spreads.containsKey("BBB") && spreads.containsKey("B")) {
        assertTrue(spreads.get("B").compareTo(spreads.get("BBB")) > 0,
          "B spread should be higher than BBB spread");
      }
    }

    @Test
    @DisplayName("Should have all values positive")
    void testPositiveValues() {
      // All yields should be positive
      FallbackMarketData.YIELD_CURVES.values().forEach(regionData ->
        regionData.values().forEach(yield ->
          assertTrue(yield.compareTo(BigDecimal.ZERO) > 0, "All yields should be positive")));
      
      // All credit spreads should be positive
      FallbackMarketData.CREDIT_SPREADS.values().forEach(spread ->
        assertTrue(spread.compareTo(BigDecimal.ZERO) > 0, "All credit spreads should be positive"));
      
      // All benchmark rates should be positive
      FallbackMarketData.BENCHMARK_RATES.values().forEach(rate ->
        assertTrue(rate.compareTo(BigDecimal.ZERO) > 0, "All benchmark rates should be positive"));
    }
  }
}