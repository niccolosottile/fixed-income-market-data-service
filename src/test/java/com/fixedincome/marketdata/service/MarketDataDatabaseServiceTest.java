package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;
import com.fixedincome.marketdata.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataDatabaseService Tests")
class MarketDataDatabaseServiceTest {

  @Mock
  private MarketDataRepository marketDataRepository;

  private MarketDataDatabaseService databaseService;

  @BeforeEach
  void setUp() {
    databaseService = new MarketDataDatabaseService(marketDataRepository);
  }

  @Nested
  @DisplayName("Yield Curve Database Operations")
  class YieldCurveOperationsTest {

    @Test
    @DisplayName("Should store yield curve data successfully")
    void testStoreYieldCurve_Success() {
      // Given
      YieldCurveResponse yieldCurve = createMockYieldCurveResponse();
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<MarketData>> marketDataCaptor = ArgumentCaptor.forClass(List.class);

      // When
      databaseService.storeYieldCurve(yieldCurve);

      // Then
      verify(marketDataRepository).saveAll(marketDataCaptor.capture());
      List<MarketData> savedData = marketDataCaptor.getValue();
      
      assertEquals(3, savedData.size());
      assertEquals(MarketData.DataType.YIELD_CURVE, savedData.get(0).getDataType());
      assertEquals("FRED", savedData.get(0).getSource());
      assertEquals(yieldCurve.getDate(), savedData.get(0).getDataDate());
    }

    @Test
    @DisplayName("Should handle store yield curve exception gracefully")
    void testStoreYieldCurve_Exception() {
      // Given
      YieldCurveResponse yieldCurve = createMockYieldCurveResponse();
      doThrow(new RuntimeException("Database error")).when(marketDataRepository).saveAll(any());

      // When & Then
      assertDoesNotThrow(() -> databaseService.storeYieldCurve(yieldCurve));
    }

    @Test
    @DisplayName("Should retrieve latest yield curve from database")
    void testGetLatestYieldCurve_Success() {
      // Given
      List<MarketData> mockData = createMockMarketDataList();
      when(marketDataRepository.findLatestByDataTypeAsOfDate(
        eq(MarketData.DataType.YIELD_CURVE), any(LocalDate.class)))
        .thenReturn(mockData);

      // When
      Optional<YieldCurveResponse> result = databaseService.getLatestYieldCurve();

      // Then
      assertTrue(result.isPresent());
      assertEquals("TEST_SOURCE", result.get().getSource());
      assertEquals(2, result.get().getYields().size());
      assertTrue(result.get().getYields().containsKey("10Y"));
    }

    @Test
    @DisplayName("Should return empty when no latest yield curve data")
    void testGetLatestYieldCurve_NoData() {
      // Given
      when(marketDataRepository.findLatestByDataTypeAsOfDate(any(), any()))
        .thenReturn(List.of());

      // When
      Optional<YieldCurveResponse> result = databaseService.getLatestYieldCurve();

      // Then
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should handle database exception gracefully")
    void testGetLatestYieldCurve_Exception() {
      // Given
      when(marketDataRepository.findLatestByDataTypeAsOfDate(any(), any()))
        .thenThrow(new RuntimeException("Database error"));

      // When
      Optional<YieldCurveResponse> result = databaseService.getLatestYieldCurve();

      // Then
      assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should retrieve historical yield curve for specific date")
    void testGetHistoricalYieldCurve_Success() {
      // Given
      LocalDate testDate = LocalDate.of(2024, 1, 15);
      List<MarketData> mockData = createMockMarketDataList();
      when(marketDataRepository.findLatestByDataTypeAsOfDate(
        eq(MarketData.DataType.YIELD_CURVE), eq(testDate)))
        .thenReturn(mockData);

      // When
      Optional<YieldCurveResponse> result = databaseService.getHistoricalYieldCurve(testDate);

      // Then
      assertTrue(result.isPresent());
      assertEquals("TEST_SOURCE", result.get().getSource());
    }

    @Test
    @DisplayName("Should retrieve yield curves for multiple dates")
    void testGetYieldCurvesForDates_Success() {
      // Given
      List<LocalDate> dates = List.of(
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 1, 2)
      );
      List<MarketData> mockData = createMockMarketDataList();
      when(marketDataRepository.findLatestByDataTypeAsOfDate(any(), any()))
        .thenReturn(mockData);

      // When
      List<YieldCurveResponse> result = databaseService.getYieldCurvesForDates(dates);

      // Then
      assertEquals(2, result.size());
      assertEquals("TEST_SOURCE", result.get(0).getSource());
    }

    @Test
    @DisplayName("Should retrieve time series data for tenor")
    void testGetYieldTimeSeries_Success() {
      // Given
      String tenor = "10Y";
      LocalDate startDate = LocalDate.of(2024, 1, 1);
      LocalDate endDate = LocalDate.of(2024, 1, 31);
      List<MarketData> mockData = createMockMarketDataList();
      when(marketDataRepository.findByDataTypeAndDataKeyAndDataDateBetweenOrderByDataDateAsc(
        eq(MarketData.DataType.YIELD_CURVE), eq(tenor), eq(startDate), eq(endDate)))
        .thenReturn(mockData);

      // When
      List<MarketData> result = databaseService.getYieldTimeSeries(tenor, startDate, endDate);

      // Then
      assertEquals(2, result.size());
      assertEquals(tenor, result.get(0).getTenor());
    }

    @Test
    @DisplayName("Should handle time series exception gracefully")
    void testGetYieldTimeSeries_Exception() {
      // Given
      when(marketDataRepository.findByDataTypeAndDataKeyAndDataDateBetweenOrderByDataDateAsc(
        any(), anyString(), any(), any()))
        .thenThrow(new RuntimeException("Database error"));

      // When
      List<MarketData> result = databaseService.getYieldTimeSeries("10Y", 
        LocalDate.now().minusDays(1), LocalDate.now());

      // Then
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Credit Spreads Database Operations")
  class CreditSpreadsOperationsTest {

    @Test
    @DisplayName("Should store credit spreads successfully")
    void testStoreCreditSpreads_Success() {
      // Given
      Map<String, BigDecimal> spreads = Map.of(
        "AAA", new BigDecimal("25"),
        "BBB", new BigDecimal("150")
      );
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<MarketData>> marketDataCaptor = ArgumentCaptor.forClass(List.class);

      // When
      databaseService.storeCreditSpreads(spreads, "TEST_SOURCE");

      // Then
      verify(marketDataRepository).saveAll(marketDataCaptor.capture());
      List<MarketData> savedData = marketDataCaptor.getValue();
      
      assertEquals(2, savedData.size());
      assertEquals(MarketData.DataType.CREDIT_SPREAD, savedData.get(0).getDataType());
      assertEquals("TEST_SOURCE", savedData.get(0).getSource());
    }

    @Test
    @DisplayName("Should retrieve latest credit spreads from database")
    void testGetLatestCreditSpreads_Success() {
      // Given
      List<MarketData> mockData = List.of(
        createMockMarketData("AAA", new BigDecimal("25"), MarketData.DataType.CREDIT_SPREAD),
        createMockMarketData("BBB", new BigDecimal("150"), MarketData.DataType.CREDIT_SPREAD)
      );
      when(marketDataRepository.findLatestByDataTypeAsOfDate(
        eq(MarketData.DataType.CREDIT_SPREAD), any(LocalDate.class)))
        .thenReturn(mockData);

      // When
      Optional<Map<String, BigDecimal>> result = databaseService.getLatestCreditSpreads();

      // Then
      assertTrue(result.isPresent());
      assertEquals(2, result.get().size());
      assertEquals(new BigDecimal("25"), result.get().get("AAA"));
      assertEquals(new BigDecimal("150"), result.get().get("BBB"));
    }

    @Test
    @DisplayName("Should return empty when no credit spreads data")
    void testGetLatestCreditSpreads_NoData() {
      // Given
      when(marketDataRepository.findLatestByDataTypeAsOfDate(any(), any()))
        .thenReturn(List.of());

      // When
      Optional<Map<String, BigDecimal>> result = databaseService.getLatestCreditSpreads();

      // Then
      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("Benchmark Rates Database Operations")
  class BenchmarkRatesOperationsTest {

    @Test
    @DisplayName("Should store benchmark rates successfully")
    void testStoreBenchmarkRates_Success() {
      // Given
      Map<String, BigDecimal> rates = Map.of(
        "US", new BigDecimal("5.25"),
        "EUR", new BigDecimal("4.50")
      );
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<MarketData>> marketDataCaptor = ArgumentCaptor.forClass(List.class);

      // When
      databaseService.storeBenchmarkRates(rates, "TEST_SOURCE");

      // Then
      verify(marketDataRepository).saveAll(marketDataCaptor.capture());
      List<MarketData> savedData = marketDataCaptor.getValue();
      
      assertEquals(2, savedData.size());
      assertEquals(MarketData.DataType.BENCHMARK_RATE, savedData.get(0).getDataType());
    }

    @Test
    @DisplayName("Should retrieve latest benchmark rates from database")
    void testGetLatestBenchmarkRates_Success() {
      // Given
      List<MarketData> mockData = List.of(
        createMockMarketData("US", new BigDecimal("5.25"), MarketData.DataType.BENCHMARK_RATE),
        createMockMarketData("EUR", new BigDecimal("4.50"), MarketData.DataType.BENCHMARK_RATE)
      );
      when(marketDataRepository.findLatestByDataTypeAsOfDate(
        eq(MarketData.DataType.BENCHMARK_RATE), any(LocalDate.class)))
        .thenReturn(mockData);

      // When
      Optional<Map<String, BigDecimal>> result = databaseService.getLatestBenchmarkRates();

      // Then
      assertTrue(result.isPresent());
      assertEquals(2, result.get().size());
      assertEquals(new BigDecimal("5.25"), result.get().get("US"));
    }
  }

  @Nested
  @DisplayName("Utility Methods")
  class UtilityMethodsTest {

    @Test
    @DisplayName("Should get yield for specific tenor")
    void testGetYieldForTenor_Success() {
      // Given
      String tenor = "10Y";
      LocalDate date = LocalDate.now();
      MarketData mockData = createMockMarketData(tenor, new BigDecimal("4.25"), MarketData.DataType.YIELD_CURVE);
      when(marketDataRepository.findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
        eq(MarketData.DataType.YIELD_CURVE), eq(tenor), eq(date)))
        .thenReturn(Optional.of(mockData));

      // When
      Optional<BigDecimal> result = databaseService.getYieldForTenor(tenor, date);

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("4.25"), result.get());
    }

    @Test
    @DisplayName("Should get credit spread for rating")
    void testGetCreditSpreadForRating_Success() {
      // Given
      String rating = "BBB";
      MarketData mockData = createMockMarketData(rating, new BigDecimal("150"), MarketData.DataType.CREDIT_SPREAD);
      when(marketDataRepository.findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
        eq(MarketData.DataType.CREDIT_SPREAD), eq(rating), any(LocalDate.class)))
        .thenReturn(Optional.of(mockData));

      // When
      Optional<BigDecimal> result = databaseService.getCreditSpreadForRating(rating);

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("150"), result.get());
    }

    @Test
    @DisplayName("Should get benchmark rate for region")
    void testGetBenchmarkRateForRegion_Success() {
      // Given
      String region = "US";
      MarketData mockData = createMockMarketData(region, new BigDecimal("5.25"), MarketData.DataType.BENCHMARK_RATE);
      when(marketDataRepository.findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
        eq(MarketData.DataType.BENCHMARK_RATE), eq(region), any(LocalDate.class)))
        .thenReturn(Optional.of(mockData));

      // When
      Optional<BigDecimal> result = databaseService.getBenchmarkRateForRegion(region);

      // Then
      assertTrue(result.isPresent());
      assertEquals(new BigDecimal("5.25"), result.get());
    }

    @Test
    @DisplayName("Should clean old data successfully")
    void testCleanOldData_Success() {
      // Given
      LocalDate cutoffDate = LocalDate.now().minusDays(30);

      // When
      databaseService.cleanOldData(cutoffDate);

      // Then
      verify(marketDataRepository).deleteByDataDateBefore(cutoffDate);
    }

    @Test
    @DisplayName("Should handle clean old data exception gracefully")
    void testCleanOldData_Exception() {
      // Given
      LocalDate cutoffDate = LocalDate.now().minusDays(30);
      doThrow(new RuntimeException("Database error")).when(marketDataRepository).deleteByDataDateBefore(any());

      // When & Then
      assertDoesNotThrow(() -> databaseService.cleanOldData(cutoffDate));
    }
  }

  // Helper methods
  private YieldCurveResponse createMockYieldCurveResponse() {
    return YieldCurveResponse.builder()
      .date(LocalDate.now())
      .source("FRED")
      .yields(Map.of(
        "5Y", new BigDecimal("4.00"),
        "10Y", new BigDecimal("4.25"),
        "30Y", new BigDecimal("4.50")
      ))
      .lastUpdated(LocalDate.now())
      .build();
  }

  private List<MarketData> createMockMarketDataList() {
    return List.of(
      createMockMarketData("10Y", new BigDecimal("4.25"), MarketData.DataType.YIELD_CURVE),
      createMockMarketData("5Y", new BigDecimal("4.00"), MarketData.DataType.YIELD_CURVE)
    );
  }

  private MarketData createMockMarketData(String key, BigDecimal value, MarketData.DataType dataType) {
    return MarketData.builder()
      .dataType(dataType)
      .dataKey(key)
      .dataValue(value)
      .dataDate(LocalDate.now())
      .source("TEST_SOURCE")
      .currency("EUR")
      .tenor(dataType == MarketData.DataType.YIELD_CURVE ? key : null)
      .build();
  }
}