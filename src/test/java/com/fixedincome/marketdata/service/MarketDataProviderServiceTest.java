package com.fixedincome.marketdata.service;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataProviderService Tests")
class MarketDataProviderServiceTest {

  @Mock
  private MarketDataProvider fredProvider;

  @Mock
  private MarketDataProvider alternativeProvider;

  private MarketDataProviderService providerService;

  @BeforeEach
  void setUp() {
    lenient().when(fredProvider.getProviderName()).thenReturn("FRED");
    lenient().when(alternativeProvider.getProviderName()).thenReturn("ALTERNATIVE_API");
    
    providerService = new MarketDataProviderService(fredProvider, Optional.of(alternativeProvider));
  }

  @Nested
  @DisplayName("Yield Curve Provider Operations")
  class YieldCurveProviderOperationsTest {

    @Test
    @DisplayName("Should fetch latest yield curve from FRED when healthy")
    void testFetchLatestYieldCurve_FredHealthy() {
      // Given
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("FRED");
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenReturn(expectedResponse);

      // When
      Optional<YieldCurveResponse> result = providerService.fetchLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("FRED", result.get().getSource());
      verify(fredProvider).fetchLatestYieldCurve();
      verify(alternativeProvider, never()).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should fallback to alternative provider when FRED unhealthy")
    void testFetchLatestYieldCurve_FredUnhealthyFallback() {
      // Given
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("ALTERNATIVE_API");
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.fetchLatestYieldCurve()).thenReturn(expectedResponse);

      // When
      Optional<YieldCurveResponse> result = providerService.fetchLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("ALTERNATIVE_API", result.get().getSource());
      verify(fredProvider, never()).fetchLatestYieldCurve();
      verify(alternativeProvider).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should fallback to alternative when FRED throws exception")
    void testFetchLatestYieldCurve_FredExceptionFallback() {
      // Given
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("ALTERNATIVE_API");
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("FRED API Error"));
      when(alternativeProvider.fetchLatestYieldCurve()).thenReturn(expectedResponse);

      // When
      Optional<YieldCurveResponse> result = providerService.fetchLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("ALTERNATIVE_API", result.get().getSource());
      verify(fredProvider).fetchLatestYieldCurve();
      verify(alternativeProvider).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should return empty when all providers fail")
    void testFetchLatestYieldCurve_AllProvidersFail() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("FRED API Error"));
      when(alternativeProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("Alternative API Error"));

      // When
      Optional<YieldCurveResponse> result = providerService.fetchLatestYieldCurve();

      // Then
      assertFalse(result.isPresent());
      verify(fredProvider).fetchLatestYieldCurve();
      verify(alternativeProvider).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should fetch historical yield curve from FRED first")
    void testFetchHistoricalYieldCurve_Success() {
      // Given
      LocalDate testDate = LocalDate.of(2024, 1, 15);
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("FRED");
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchHistoricalYieldCurve(testDate)).thenReturn(expectedResponse);

      // When
      Optional<YieldCurveResponse> result = providerService.fetchHistoricalYieldCurve(testDate);

      // Then
      assertTrue(result.isPresent());
      assertEquals("FRED", result.get().getSource());
      verify(fredProvider).fetchHistoricalYieldCurve(testDate);
    }

    @Test
    @DisplayName("Should fetch yield curves for multiple dates")
    void testFetchYieldCurvesForDates_Success() {
      // Given
      List<LocalDate> dates = List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));
      List<YieldCurveResponse> expectedResponses = dates.stream()
        .map(date -> createMockYieldCurveResponseForDate("FRED", date))
        .toList();
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchYieldCurvesForDates(dates)).thenReturn(expectedResponses);

      // When
      Optional<List<YieldCurveResponse>> result = providerService.fetchYieldCurvesForDates(dates);

      // Then
      assertTrue(result.isPresent());
      assertEquals(2, result.get().size());
      verify(fredProvider).fetchYieldCurvesForDates(dates);
    }

    @Test
    @DisplayName("Should fetch yield time series")
    void testFetchYieldTimeSeries_Success() {
      // Given
      String tenor = "10Y";
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);
      List<MarketData> expectedData = List.of(createMockMarketData("10Y", new BigDecimal("4.25")));
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchYieldTimeSeries(tenor, startDate, endDate)).thenReturn(expectedData);

      // When
      Optional<List<MarketData>> result = providerService.fetchYieldTimeSeries(tenor, startDate, endDate);

      // Then
      assertTrue(result.isPresent());
      assertEquals(1, result.get().size());
      verify(fredProvider).fetchYieldTimeSeries(tenor, startDate, endDate);
    }
  }

  @Nested
  @DisplayName("Credit and Market Data Provider Operations")
  class CreditAndMarketDataProviderOperationsTest {

    @Test
    @DisplayName("Should fetch credit spreads from alternative provider")
    void testFetchCreditSpreads_Success() {
      // Given
      Map<String, BigDecimal> expectedSpreads = Map.of("BBB", new BigDecimal("150"));
      when(alternativeProvider.fetchCreditSpreads()).thenReturn(expectedSpreads);

      // When
      Optional<Map<String, BigDecimal>> result = providerService.fetchCreditSpreads();

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("150"), result.get().get("BBB"));
      verify(alternativeProvider).fetchCreditSpreads();
    }

    @Test
    @DisplayName("Should handle credit spreads exception gracefully")
    void testFetchCreditSpreads_Exception() {
      // Given
      when(alternativeProvider.fetchCreditSpreads()).thenThrow(new RuntimeException("API Error"));

      // When
      Optional<Map<String, BigDecimal>> result = providerService.fetchCreditSpreads();

      // Then
      assertFalse(result.isPresent());
      verify(alternativeProvider).fetchCreditSpreads();
    }

    @Test
    @DisplayName("Should fetch benchmark rates from alternative provider")
    void testFetchBenchmarkRates_Success() {
      // Given
      Map<String, BigDecimal> expectedRates = Map.of("US", new BigDecimal("5.25"));
      when(alternativeProvider.fetchBenchmarkRates()).thenReturn(expectedRates);

      // When
      Optional<Map<String, BigDecimal>> result = providerService.fetchBenchmarkRates();

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("5.25"), result.get().get("US"));
      verify(alternativeProvider).fetchBenchmarkRates();
    }

    @Test
    @DisplayName("Should fetch inflation expectations from alternative provider")
    void testFetchInflationExpectations_Success() {
      // Given
      String region = "US";
      Map<String, BigDecimal> expectedExpectations = Map.of("10Y", new BigDecimal("2.5"));
      when(alternativeProvider.fetchInflationExpectations(region)).thenReturn(expectedExpectations);

      // When
      Optional<Map<String, BigDecimal>> result = providerService.fetchInflationExpectations(region);

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("2.5"), result.get().get("10Y"));
      verify(alternativeProvider).fetchInflationExpectations(region);
    }

    @Test
    @DisplayName("Should fetch sector credit data from alternative provider")
    void testFetchSectorCreditData_Success() {
      // Given
      String sector = "TECH";
      Map<String, BigDecimal> expectedData = Map.of("BBB", new BigDecimal("120"));
      when(alternativeProvider.fetchSectorCreditData(sector)).thenReturn(expectedData);

      // When
      Optional<Map<String, BigDecimal>> result = providerService.fetchSectorCreditData(sector);

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("120"), result.get().get("BBB"));
      verify(alternativeProvider).fetchSectorCreditData(sector);
    }

    @Test
    @DisplayName("Should fetch liquidity premiums from alternative provider")
    void testFetchLiquidityPremiums_Success() {
      // Given
      Map<String, BigDecimal> expectedPremiums = Map.of("CORPORATE", new BigDecimal("15"));
      when(alternativeProvider.fetchLiquidityPremiums()).thenReturn(expectedPremiums);

      // When
      Optional<Map<String, BigDecimal>> result = providerService.fetchLiquidityPremiums();

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("15"), result.get().get("CORPORATE"));
      verify(alternativeProvider).fetchLiquidityPremiums();
    }
  }

  @Nested
  @DisplayName("Health Monitoring Operations")
  class HealthMonitoringOperationsTest {

    @Test
    @DisplayName("Should return true when any provider is healthy")
    void testIsAnyProviderHealthy_FredHealthy() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(alternativeProvider.isServiceHealthy()).thenReturn(false);

      // When
      boolean result = providerService.isAnyProviderHealthy();

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should return true when alternative provider is healthy")
    void testIsAnyProviderHealthy_AlternativeHealthy() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.isServiceHealthy()).thenReturn(true);

      // When
      boolean result = providerService.isAnyProviderHealthy();

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when no providers are healthy")
    void testIsAnyProviderHealthy_NoneHealthy() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);
      when(alternativeProvider.isServiceHealthy()).thenReturn(false);

      // When
      boolean result = providerService.isAnyProviderHealthy();

      // Then
      assertFalse(result);
    }

    @Test
    @DisplayName("Should return provider health status map")
    void testGetProviderHealthStatus_Success() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(alternativeProvider.isServiceHealthy()).thenReturn(false);

      // When
      Map<String, Boolean> result = providerService.getProviderHealthStatus();

      // Then
      assertEquals(2, result.size());
      assertTrue(result.get("FRED"));
      assertFalse(result.get("ALTERNATIVE_API"));
    }

    @Test
    @DisplayName("Should handle provider health check exceptions")
    void testGetProviderHealthStatus_WithExceptions() {
      // Given
      when(fredProvider.isServiceHealthy()).thenThrow(new RuntimeException("Connection error"));
      when(alternativeProvider.isServiceHealthy()).thenReturn(true);

      // When
      Map<String, Boolean> result = providerService.getProviderHealthStatus();

      // Then
      assertEquals(2, result.size());
      assertFalse(result.get("FRED")); // Should be false when exception occurs
      assertTrue(result.get("ALTERNATIVE_API"));
    }
  }

  @Nested
  @DisplayName("Service Without Alternative Provider")
  class ServiceWithoutAlternativeTest {

    private MarketDataProviderService serviceWithoutAlternative;

    @BeforeEach
    void setUp() {
      serviceWithoutAlternative = new MarketDataProviderService(fredProvider, Optional.empty());
    }

    @Test
    @DisplayName("Should work with only FRED provider")
    void testFetchLatestYieldCurve_OnlyFred() {
      // Given
      YieldCurveResponse expectedResponse = createMockYieldCurveResponse("FRED");
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenReturn(expectedResponse);

      // When
      Optional<YieldCurveResponse> result = serviceWithoutAlternative.fetchLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("FRED", result.get().getSource());
      verify(fredProvider).fetchLatestYieldCurve();
    }

    @Test
    @DisplayName("Should return empty when only FRED fails and no alternative")
    void testFetchLatestYieldCurve_OnlyFredFails() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);

      // When
      Optional<YieldCurveResponse> result = serviceWithoutAlternative.fetchLatestYieldCurve();

      // Then
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return false for health when only FRED unhealthy")
    void testIsAnyProviderHealthy_OnlyFredUnhealthy() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(false);

      // When
      boolean result = serviceWithoutAlternative.isAnyProviderHealthy();

      // Then
      assertFalse(result);
    }

    @Test
    @DisplayName("Should return single provider health status")
    void testGetProviderHealthStatus_OnlyFred() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);

      // When
      Map<String, Boolean> result = serviceWithoutAlternative.getProviderHealthStatus();

      // Then
      assertEquals(1, result.size());
      assertTrue(result.get("FRED"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesTest {

    @Test
    @DisplayName("Should handle null response from provider gracefully")
    void testFetchLatestYieldCurve_NullResponse() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenReturn(null);
      when(alternativeProvider.fetchLatestYieldCurve()).thenReturn(createMockYieldCurveResponse("ALTERNATIVE_API"));

      // When
      Optional<YieldCurveResponse> result = providerService.fetchLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("ALTERNATIVE_API", result.get().getSource());
    }

    @Test
    @DisplayName("Should handle empty list from provider gracefully")
    void testFetchYieldCurvesForDates_EmptyResponse() {
      // Given
      List<LocalDate> dates = List.of(LocalDate.of(2024, 1, 1));
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchYieldCurvesForDates(dates)).thenReturn(List.of());
      when(alternativeProvider.fetchYieldCurvesForDates(dates))
        .thenReturn(List.of(createMockYieldCurveResponse("ALTERNATIVE_API")));

      // When
      Optional<List<YieldCurveResponse>> result = providerService.fetchYieldCurvesForDates(dates);

      // Then
      assertTrue(result.isPresent());
      assertEquals(1, result.get().size());
      assertEquals("ALTERNATIVE_API", result.get().get(0).getSource());
    }

    @Test
    @DisplayName("Should handle provider timing out")
    void testFetchLatestYieldCurve_ProviderTimeout() {
      // Given
      when(fredProvider.isServiceHealthy()).thenReturn(true);
      when(fredProvider.fetchLatestYieldCurve()).thenThrow(new RuntimeException("Timeout"));
      when(alternativeProvider.fetchLatestYieldCurve()).thenReturn(createMockYieldCurveResponse("ALTERNATIVE_API"));

      // When
      Optional<YieldCurveResponse> result = providerService.fetchLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("ALTERNATIVE_API", result.get().getSource());
    }
  }

  // Helper methods
  private YieldCurveResponse createMockYieldCurveResponse(String source) {
    return YieldCurveResponse.builder()
      .date(LocalDate.now())
      .source(source)
      .yields(Map.of(
        "5Y", new BigDecimal("4.00"),
        "10Y", new BigDecimal("4.25"),
        "30Y", new BigDecimal("4.50")
      ))
      .lastUpdated(LocalDate.now())
      .build();
  }

  private YieldCurveResponse createMockYieldCurveResponseForDate(String source, LocalDate date) {
    return YieldCurveResponse.builder()
      .date(date)
      .source(source)
      .yields(Map.of(
        "5Y", new BigDecimal("4.00"),
        "10Y", new BigDecimal("4.25"),
        "30Y", new BigDecimal("4.50")
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