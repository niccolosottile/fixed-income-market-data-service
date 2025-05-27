package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.config.FallbackMarketData;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;
import com.fixedincome.marketdata.service.integration.MarketDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Tests")
class MarketDataServiceTest {

  @Mock
  private MarketDataProvider fredProvider;

  @Mock
  private MarketDataProvider alternativeProvider;

  private MarketDataService marketDataService;

  @BeforeEach
  void setUp() {
    // Use lenient stubbing to avoid UnnecessaryStubbingException
    lenient().when(fredProvider.getProviderName()).thenReturn("FRED");
    lenient().when(alternativeProvider.getProviderName()).thenReturn("ALTERNATIVE_API");
    lenient().when(fredProvider.getSupportedTenors()).thenReturn(Set.of("1Y", "2Y", "5Y", "10Y", "30Y"));
    
    marketDataService = new MarketDataService(fredProvider, Optional.of(alternativeProvider));
  }

  @Nested
  @DisplayName("Yield Curve Operations")
  class YieldCurveOperationsTest {

    @Test
    @DisplayName("Should get latest yield curve from FRED when healthy")
    void testGetLatestYieldCurve_WithHealthyFredProvider() {
      // Given
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("FRED", LocalDate.now());
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenReturn(expectedResponse);

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("FRED", result.getSource());
      assertEquals(LocalDate.now(), result.getDate());
      verify(fredProvider).fetchLatestYieldCurve();
      verify(alternativeProvider, never()).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should fallback to alternative provider when FRED fails")
    void testGetLatestYieldCurve_FallbackToAlternativeProvider() {
      // Given
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("ALTERNATIVE_API", LocalDate.now());
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchLatestYieldCurve()).thenReturn(expectedResponse);

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("ALTERNATIVE_API", result.getSource());
      verify(alternativeProvider).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should use fallback data when all providers fail")
    void testGetLatestYieldCurve_FallbackToStaticData() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("API down"));

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
      assertNotNull(result.getYields());
      assertTrue(result.getYields().containsKey("10Y"));
    }

    @Test
    @DisplayName("Should get historical yield curve for specific date")
    void testGetHistoricalYieldCurve_Success() {
      // Given
      LocalDate testDate = LocalDate.of(2024, 1, 15);
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("FRED", testDate);
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchHistoricalYieldCurve(testDate)).thenReturn(expectedResponse);

      // When
      YieldCurveResponse result = marketDataService.getHistoricalYieldCurve(testDate);

      // Then
      assertNotNull(result);
      assertEquals("FRED", result.getSource());
      assertEquals(testDate, result.getDate());
      verify(fredProvider).fetchHistoricalYieldCurve(testDate);
    }

    @Test
    @DisplayName("Should get yield curves for multiple dates")
    void testGetYieldCurvesForDates_Success() {
      // Given
      List<LocalDate> dates = List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));
      List<YieldCurveResponse> expectedResponses = dates.stream()
        .map(date -> createMockYieldCurveResponse("FRED", date))
        .toList();
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchYieldCurvesForDates(dates)).thenReturn(expectedResponses);

      // When
      List<YieldCurveResponse> result = marketDataService.getYieldCurvesForDates(dates);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals("FRED", result.get(0).getSource());
      verify(fredProvider).fetchYieldCurvesForDates(dates);
    }

    @Test
    @DisplayName("Should get yield time series for tenor")
    void testGetYieldTimeSeries_Success() {
      // Given
      String tenor = "10Y";
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);
      List<MarketData> expectedData = List.of(createMockMarketData(tenor, new BigDecimal("4.25")));
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchYieldTimeSeries(tenor, startDate, endDate)).thenReturn(expectedData);

      // When
      List<MarketData> result = marketDataService.getYieldTimeSeries(tenor, startDate, endDate);

      // Then
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(tenor, result.get(0).getTenor());
      verify(fredProvider).fetchYieldTimeSeries(tenor, startDate, endDate);
    }

    @Test
    @DisplayName("Should return empty list when time series data unavailable")
    void testGetYieldTimeSeries_Fallback() {
      // Given
      String tenor = "10Y";
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchYieldTimeSeries(anyString(), any(), any()))
        .thenThrow(new RuntimeException("API down"));

      // When
      List<MarketData> result = marketDataService.getYieldTimeSeries(tenor, startDate, endDate);

      // Then
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Credit and Spread Operations")
  class CreditAndSpreadOperationsTest {

    @Test
    @DisplayName("Should get credit spreads from alternative provider")
    void testGetCreditSpreads_Success() {
      // Given
      Map<String, BigDecimal> expectedSpreads = Map.of(
        "AAA", new BigDecimal("25"),
        "BBB", new BigDecimal("150")
      );
      when(alternativeProvider.fetchCreditSpreads()).thenReturn(expectedSpreads);

      // When
      Map<String, BigDecimal> result = marketDataService.getCreditSpreads();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("25"), result.get("AAA"));
      assertEquals(new BigDecimal("150"), result.get("BBB"));
      verify(alternativeProvider).fetchCreditSpreads();
    }

    @Test
    @DisplayName("Should use fallback credit spreads when provider fails")
    void testGetCreditSpreads_Fallback() {
      // Given
      when(alternativeProvider.fetchCreditSpreads()).thenThrow(new RuntimeException("API down"));

      // When
      Map<String, BigDecimal> result = marketDataService.getCreditSpreads();

      // Then
      assertNotNull(result);
      assertEquals(FallbackMarketData.CREDIT_SPREADS.get("BBB"), result.get("BBB"));
    }

    @Test
    @DisplayName("Should get inflation expectations for region")
    void testGetInflationExpectations_Success() {
      // Given
      String region = "US";
      Map<String, BigDecimal> expectedExpectations = Map.of("10Y", new BigDecimal("2.5"));
      when(alternativeProvider.fetchInflationExpectations(region)).thenReturn(expectedExpectations);

      // When
      Map<String, BigDecimal> result = marketDataService.getInflationExpectations(region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("2.5"), result.get("10Y"));
      verify(alternativeProvider).fetchInflationExpectations(region);
    }

    @Test
    @DisplayName("Should get benchmark rates")
    void testGetBenchmarkRates_Success() {
      // Given
      Map<String, BigDecimal> expectedRates = Map.of("US", new BigDecimal("5.25"));
      when(alternativeProvider.fetchBenchmarkRates()).thenReturn(expectedRates);

      // When
      Map<String, BigDecimal> result = marketDataService.getBenchmarkRates();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("5.25"), result.get("US"));
      verify(alternativeProvider).fetchBenchmarkRates();
    }

    @Test
    @DisplayName("Should get sector credit data")
    void testGetSectorCreditData_Success() {
      // Given
      String sector = "TECH";
      Map<String, BigDecimal> expectedData = Map.of("BBB", new BigDecimal("120"));
      when(alternativeProvider.fetchSectorCreditData(sector)).thenReturn(expectedData);

      // When
      Map<String, BigDecimal> result = marketDataService.getSectorCreditData(sector);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("120"), result.get("BBB"));
      verify(alternativeProvider).fetchSectorCreditData(sector);
    }

    @Test
    @DisplayName("Should get liquidity premiums")
    void testGetLiquidityPremiums_Success() {
      // Given
      Map<String, BigDecimal> expectedPremiums = Map.of("CORPORATE", new BigDecimal("15"));
      when(alternativeProvider.fetchLiquidityPremiums()).thenReturn(expectedPremiums);

      // When
      Map<String, BigDecimal> result = marketDataService.getLiquidityPremiums();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("15"), result.get("CORPORATE"));
      verify(alternativeProvider).fetchLiquidityPremiums();
    }
  }

  @Nested
  @DisplayName("Convenience Methods for Pricing Engine")
  class ConvenienceMethodsTest {

    @Test
    @DisplayName("Should get yield for specific tenor and region")
    void testGetYieldForTenor_Success() {
      // Given
      String tenor = "10Y";
      String region = "EUR";
      YieldCurveResponse mockCurve = createMockYieldCurveResponse("FRED", LocalDate.now());
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenReturn(mockCurve);

      // When
      BigDecimal result = marketDataService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("4.23"), result);
    }

    @Test
    @DisplayName("Should fallback to static data for yield when providers fail")
    void testGetYieldForTenor_Fallback() {
      // Given
      String tenor = "10Y";
      String region = "EUR";
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("API down"));

      // When
      BigDecimal result = marketDataService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), result);
    }

    @Test
    @DisplayName("Should get credit spread for specific rating")
    void testGetCreditSpreadForRating_Success() {
      // Given
      String rating = "BBB";
      Map<String, BigDecimal> mockSpreads = Map.of("BBB", new BigDecimal("150"));
      when(alternativeProvider.fetchCreditSpreads()).thenReturn(mockSpreads);

      // When
      BigDecimal result = marketDataService.getCreditSpreadForRating(rating);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("150"), result);
    }

    @Test
    @DisplayName("Should get benchmark rate for specific region")
    void testGetBenchmarkRateForRegion_Success() {
      // Given
      String region = "US";
      Map<String, BigDecimal> mockRates = Map.of("US", new BigDecimal("5.25"));
      when(alternativeProvider.fetchBenchmarkRates()).thenReturn(mockRates);

      // When
      BigDecimal result = marketDataService.getBenchmarkRateForRegion(region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("5.25"), result);
    }
  }

  @Nested
  @DisplayName("Metadata and Health Operations")
  class MetadataAndHealthTest {

    @Test
    @DisplayName("Should indicate market data available when FRED is healthy")
    void testIsMarketDataAvailable_WithHealthyFredProvider() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);

      // When
      boolean result = marketDataService.isMarketDataAvailable();

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should indicate market data available when alternative is healthy")
    void testIsMarketDataAvailable_WithHealthyAlternativeProvider() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.isServiceHealthy()).thenReturn(true);

      // When
      boolean result = marketDataService.isMarketDataAvailable();

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should indicate market data unavailable when all providers unhealthy")
    void testIsMarketDataAvailable_WithNoHealthyProviders() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.isServiceHealthy()).thenReturn(false);

      // When
      boolean result = marketDataService.isMarketDataAvailable();

      // Then
      assertFalse(result);
    }

    @Test
    @DisplayName("Should get supported tenors from FRED provider")
    void testGetSupportedTenors() {
      // When
      Set<String> result = marketDataService.getSupportedTenors();

      // Then
      assertNotNull(result);
      assertEquals(5, result.size());
      assertTrue(result.contains("10Y"));
    }

    @Test
    @DisplayName("Should get provider health status")
    void testGetProviderHealthStatus() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(alternativeProvider.isServiceHealthy()).thenReturn(false);

      // When
      Map<String, Boolean> result = marketDataService.getProviderHealthStatus();

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertTrue(result.get("FRED"));
      assertFalse(result.get("ALTERNATIVE_API"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesTest {

    @Test
    @DisplayName("Should handle null region gracefully")
    void testGetYieldForTenor_WithNullRegion() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("API down"));

      // When
      BigDecimal result = marketDataService.getYieldForTenor("10Y", null);

      // Then
      assertNotNull(result);
      // Should default to EUR when region is null
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), result);
    }

    @Test
    @DisplayName("Should handle unsupported region gracefully")
    void testGetYieldForTenor_WithUnsupportedRegion() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("API down"));

      // When
      BigDecimal result = marketDataService.getYieldForTenor("10Y", "UNSUPPORTED");

      // Then
      assertNotNull(result);
      // Should default to EUR when region is not found
      assertEquals(FallbackMarketData.YIELD_CURVES.get("EUR").get("10Y"), result);
    }

    @Test
    @DisplayName("Should handle service without alternative provider")
    void testServiceWithoutAlternativeProvider() {
      // Given
      MarketDataService serviceWithoutAlt = new MarketDataService(fredProvider, Optional.empty());
      when(fredProvider.isServiceHealthy()).thenReturn(false);

      // When
      YieldCurveResponse result = serviceWithoutAlt.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
    }
  }

  // Helper methods for creating test data
  private YieldCurveResponse createMockYieldCurveResponse(String source, LocalDate date) {
    return YieldCurveResponse.builder()
      .date(date)
      .source(source)
      .yields(Map.of(
        "1Y", new BigDecimal("3.50"),
        "5Y", new BigDecimal("4.00"),
        "10Y", new BigDecimal("4.23"),
        "30Y", new BigDecimal("4.39")
      ))
      .lastUpdated(LocalDate.now())
      .build();
  }

  private MarketData createMockMarketData(String tenor, BigDecimal value) {
    return MarketData.builder()
      .dataType(MarketData.DataType.YIELD_CURVE)
      .dataKey(tenor)
      .dataValue(value)
      .dataDate(LocalDate.now())
      .source("FRED")
      .currency("EUR")
      .tenor(tenor)
      .build();
  }
}