package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.exception.DataValidationException;
import com.fixedincome.marketdata.model.MarketData;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Tests")
class MarketDataServiceTest {

  @Mock
  private MarketDataDatabaseService databaseService;
  @Mock
  private MarketDataProviderService providerService;
  @Mock
  private MarketDataFallbackService fallbackService;
  @Mock
  private CacheManagementService cacheManagementService;

  private MarketDataService marketDataService;

  @BeforeEach
  void setUp() {
    // Mock the getSupportedTenors method on the provider service instead
    lenient().when(providerService.getSupportedTenors()).thenReturn(Set.of("1Y", "2Y", "5Y", "10Y", "30Y"));
    
    marketDataService = new MarketDataService(
      databaseService,
      providerService,
      fallbackService,
      cacheManagementService
    );
  }

  @Nested
  @DisplayName("Yield Curve Operations")
  class YieldCurveOperationsTest {

    @Test
    @DisplayName("Should get latest yield curve from database when available")
    void testGetLatestYieldCurve_WithHealthyFredProvider() {
      // Given - Database has data
      YieldCurveResponse dbResponse = createMockYieldCurveResponse("DATABASE");
      when(databaseService.getLatestYieldCurve()).thenReturn(Optional.of(dbResponse));

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("DATABASE", result.getSource());
      verify(databaseService).getLatestYieldCurve();
      verify(providerService, never()).fetchLatestYieldCurve();
      verify(fallbackService, never()).createFallbackYieldCurve();
    }

    @Test
    @DisplayName("Should fallback to providers when database empty")
    void testGetLatestYieldCurve_FallbackToAlternativeProvider() {
      // Given - Database empty, provider has data
      when(databaseService.getLatestYieldCurve()).thenReturn(Optional.empty());
      YieldCurveResponse providerResponse = createMockYieldCurveResponse("FRED");
      when(providerService.fetchLatestYieldCurve()).thenReturn(Optional.of(providerResponse));

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("FRED", result.getSource());
      verify(databaseService).getLatestYieldCurve();
      verify(providerService).fetchLatestYieldCurve();
      verify(databaseService).storeYieldCurve(providerResponse); // Should store in DB
    }

    @Test
    @DisplayName("Should use fallback data when all providers fail")
    void testGetLatestYieldCurve_FallbackToStaticData() {
      // Given - All services fail or return empty
      when(databaseService.getLatestYieldCurve()).thenReturn(Optional.empty());
      when(providerService.fetchLatestYieldCurve()).thenReturn(Optional.empty());
      YieldCurveResponse fallbackResponse = createMockYieldCurveResponse("FALLBACK");
      when(fallbackService.createFallbackYieldCurve()).thenReturn(fallbackResponse);

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
      verify(databaseService).getLatestYieldCurve();
      verify(providerService).fetchLatestYieldCurve();
      verify(fallbackService).createFallbackYieldCurve();
    }

    @Test
    @DisplayName("Should get historical yield curve for specific date")
    void testGetHistoricalYieldCurve_Success() {
      // Given
      LocalDate testDate = LocalDate.of(2024, 1, 15);
      YieldCurveResponse dbResponse = createMockYieldCurveResponse("DATABASE");
      when(databaseService.getHistoricalYieldCurve(testDate)).thenReturn(Optional.of(dbResponse));

      // When
      YieldCurveResponse result = marketDataService.getHistoricalYieldCurve(testDate);

      // Then
      assertNotNull(result);
      assertEquals("DATABASE", result.getSource());
      verify(databaseService).getHistoricalYieldCurve(testDate);
    }

    @Test
    @DisplayName("Should get yield curves for multiple dates")
    void testGetYieldCurvesForDates_Success() {
      // Given
      List<LocalDate> dates = List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));
      @SuppressWarnings("unused")
      List<YieldCurveResponse> dbResponses = dates.stream()
        .map(date -> createMockYieldCurveResponse("DATABASE"))
        .toList();
      when(databaseService.getYieldCurvesForDates(dates)).thenReturn(dbResponses);

      // When
      List<YieldCurveResponse> result = marketDataService.getYieldCurvesForDates(dates);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals("DATABASE", result.get(0).getSource());
      verify(databaseService).getYieldCurvesForDates(dates);
    }

    @Test
    @DisplayName("Should get yield time series for tenor")
    void testGetYieldTimeSeries_Success() {
      // Given
      String tenor = "10Y";
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);
      List<MarketData> dbData = List.of(createMockMarketData("10Y", new BigDecimal("4.25")));
      when(databaseService.getYieldTimeSeries(tenor, startDate, endDate)).thenReturn(dbData);

      // When
      List<MarketData> result = marketDataService.getYieldTimeSeries(tenor, startDate, endDate);

      // Then
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals("DATABASE", result.get(0).getSource());
      verify(databaseService).getYieldTimeSeries(tenor, startDate, endDate);
    }

    @Test
    @DisplayName("Should return empty list when time series data unavailable")
    void testGetYieldTimeSeries_Fallback() {
      // Given
      String tenor = "10Y";
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);
      when(databaseService.getYieldTimeSeries(tenor, startDate, endDate)).thenReturn(List.of());
      when(providerService.fetchYieldTimeSeries(tenor, startDate, endDate)).thenReturn(Optional.empty());

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
    @DisplayName("Should get credit spreads from database first")
    void testGetCreditSpreads_Success() {
      // Given
      Map<String, BigDecimal> dbSpreads = Map.of("BBB", new BigDecimal("150"));
      when(databaseService.getLatestCreditSpreads()).thenReturn(Optional.of(dbSpreads));

      // When
      Map<String, BigDecimal> result = marketDataService.getCreditSpreads();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("150"), result.get("BBB"));
      verify(databaseService).getLatestCreditSpreads();
      verify(providerService, never()).fetchCreditSpreads();
    }

    @Test
    @DisplayName("Should use fallback credit spreads when provider fails")
    void testGetCreditSpreads_Fallback() {
      // Given
      when(databaseService.getLatestCreditSpreads()).thenReturn(Optional.empty());
      when(providerService.fetchCreditSpreads()).thenReturn(Optional.empty());
      Map<String, BigDecimal> fallbackSpreads = Map.of("BBB", new BigDecimal("150"));
      when(fallbackService.getFallbackCreditSpreads()).thenReturn(fallbackSpreads);

      // When
      Map<String, BigDecimal> result = marketDataService.getCreditSpreads();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("150"), result.get("BBB"));
    }

    @Test
    @DisplayName("Should get inflation expectations for region")
    void testGetInflationExpectations_Success() {
      // Given
      String region = "US";
      Map<String, BigDecimal> expectations = Map.of("10Y", new BigDecimal("2.5"));
      when(providerService.fetchInflationExpectations(region)).thenReturn(Optional.of(expectations));

      // When
      Map<String, BigDecimal> result = marketDataService.getInflationExpectations(region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("2.5"), result.get("10Y"));
      verify(providerService).fetchInflationExpectations(region);
    }

    @Test
    @DisplayName("Should get benchmark rates")
    void testGetBenchmarkRates_Success() {
      // Given
      Map<String, BigDecimal> dbRates = Map.of("US", new BigDecimal("5.25"));
      when(databaseService.getLatestBenchmarkRates()).thenReturn(Optional.of(dbRates));

      // When
      Map<String, BigDecimal> result = marketDataService.getBenchmarkRates();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("5.25"), result.get("US"));
      verify(databaseService).getLatestBenchmarkRates();
    }

    @Test
    @DisplayName("Should get sector credit data")
    void testGetSectorCreditData_Success() {
      // Given
      String sector = "TECH";
      Map<String, BigDecimal> sectorData = Map.of("BBB", new BigDecimal("120"));
      when(providerService.fetchSectorCreditData(sector)).thenReturn(Optional.of(sectorData));

      // When
      Map<String, BigDecimal> result = marketDataService.getSectorCreditData(sector);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("120"), result.get("BBB"));
      verify(providerService).fetchSectorCreditData(sector);
    }

    @Test
    @DisplayName("Should get liquidity premiums")
    void testGetLiquidityPremiums_Success() {
      // Given
      Map<String, BigDecimal> premiums = Map.of("CORPORATE", new BigDecimal("15"));
      when(providerService.fetchLiquidityPremiums()).thenReturn(Optional.of(premiums));

      // When
      Map<String, BigDecimal> result = marketDataService.getLiquidityPremiums();

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("15"), result.get("CORPORATE"));
      verify(providerService).fetchLiquidityPremiums();
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
      when(databaseService.getYieldForTenor(tenor, LocalDate.now()))
        .thenReturn(Optional.of(new BigDecimal("4.25")));

      // When
      BigDecimal result = marketDataService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("4.25"), result);
      verify(databaseService).getYieldForTenor(tenor, LocalDate.now());
    }

    @Test
    @DisplayName("Should fallback to static data for yield when providers fail")
    void testGetYieldForTenor_Fallback() {
      // Given
      String tenor = "10Y";
      String region = "EUR";
      when(databaseService.getYieldForTenor(tenor, LocalDate.now())).thenReturn(Optional.empty());
      when(fallbackService.getFallbackYieldForTenor(tenor, region)).thenReturn(new BigDecimal("4.00"));

      // When
      BigDecimal result = marketDataService.getYieldForTenor(tenor, region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("4.00"), result);
      verify(fallbackService).getFallbackYieldForTenor(tenor, region);
    }

    @Test
    @DisplayName("Should get credit spread for specific rating")
    void testGetCreditSpreadForRating_Success() {
      // Given
      String rating = "BBB";
      when(databaseService.getCreditSpreadForRating(rating))
        .thenReturn(Optional.of(new BigDecimal("150")));

      // When
      BigDecimal result = marketDataService.getCreditSpreadForRating(rating);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("150"), result);
      verify(databaseService).getCreditSpreadForRating(rating);
    }

    @Test
    @DisplayName("Should get benchmark rate for specific region")
    void testGetBenchmarkRateForRegion_Success() {
      // Given
      String region = "US";
      when(databaseService.getBenchmarkRateForRegion(region))
        .thenReturn(Optional.of(new BigDecimal("5.25")));

      // When
      BigDecimal result = marketDataService.getBenchmarkRateForRegion(region);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("5.25"), result);
      verify(databaseService).getBenchmarkRateForRegion(region);
    }
  }

  @Nested
  @DisplayName("Metadata and Health Operations")
  class MetadataAndHealthTest {

    @Test
    @DisplayName("Should indicate market data available when providers healthy")
    void testIsMarketDataAvailable_WithHealthyFredProvider() {
      // Given
      when(providerService.isAnyProviderHealthy()).thenReturn(true);

      // When
      boolean result = marketDataService.isMarketDataAvailable();

      // Then
      assertTrue(result);
      verify(providerService).isAnyProviderHealthy();
    }

    @Test
    @DisplayName("Should indicate market data available when alternative is healthy")
    void testIsMarketDataAvailable_WithHealthyAlternativeProvider() {
      // Given
      when(providerService.isAnyProviderHealthy()).thenReturn(true);

      // When
      boolean result = marketDataService.isMarketDataAvailable();

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("Should indicate market data unavailable when all providers unhealthy")
    void testIsMarketDataAvailable_WithNoHealthyProviders() {
      // Given
      when(providerService.isAnyProviderHealthy()).thenReturn(false);

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
      Map<String, Boolean> healthStatus = Map.of("FRED", true, "ALTERNATIVE", false);
      when(providerService.getProviderHealthStatus()).thenReturn(healthStatus);

      // When
      Map<String, Boolean> result = marketDataService.getProviderHealthStatus();

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
      assertTrue(result.get("FRED"));
      assertFalse(result.get("ALTERNATIVE"));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesTest {

    @Test
    @DisplayName("Should handle null region gracefully")
    void testGetYieldForTenor_WithNullRegion() {
      // Given - null region should throw validation exception
      
      // When & Then
      assertThrows(DataValidationException.class, () -> {
        marketDataService.getYieldForTenor("10Y", null);
      });
    }

    @Test
    @DisplayName("Should throw validation exception for unsupported region")
    void testGetYieldForTenor_WithUnsupportedRegion() {
      // Given - unsupported region should throw validation exception
      
      // When & Then
      assertThrows(DataValidationException.class, () -> {
        marketDataService.getYieldForTenor("10Y", "UNSUPPORTED");
      });
    }

    @Test
    @DisplayName("Should handle service without alternative provider")
    void testServiceWithoutAlternativeProvider() {
      // This test verifies that the service works when constructed with the layered architecture
      // and properly delegates to fallback when all providers fail
      YieldCurveResponse fallbackResponse = createMockYieldCurveResponse("FALLBACK");
      when(databaseService.getLatestYieldCurve()).thenReturn(Optional.empty());
      when(providerService.fetchLatestYieldCurve()).thenReturn(Optional.empty());
      when(fallbackService.createFallbackYieldCurve()).thenReturn(fallbackResponse);

      // When
      YieldCurveResponse result = marketDataService.getLatestYieldCurve();

      // Then
      assertNotNull(result);
      assertEquals("FALLBACK", result.getSource());
    }
  }

  // Helper methods for creating test data
  private YieldCurveResponse createMockYieldCurveResponse(String source) {
    return YieldCurveResponse.builder()
      .date(LocalDate.now())
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
      .source("DATABASE")
      .currency("EUR")
      .tenor(tenor)
      .build();
  }
}