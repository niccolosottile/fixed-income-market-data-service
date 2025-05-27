package com.fixedincome.marketdata.service.integration;

import com.fixedincome.marketdata.config.FredApiProperties;
import com.fixedincome.marketdata.dto.FredApiResponse;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.exception.MarketDataException;
import com.fixedincome.marketdata.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FredApiClientTest {

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private FredApiProperties fredApiProperties;

  private FredApiClient fredApiClient;

  @BeforeEach
  void setUp() {
    // Use lenient stubs for properties that might not be used in all tests
    lenient().when(fredApiProperties.getBaseUrl()).thenReturn("https://api.stlouisfed.org/fred/series/observations");
    lenient().when(fredApiProperties.getApiKey()).thenReturn("test-api-key");
    lenient().when(fredApiProperties.getConnectTimeout()).thenReturn(5000);
    lenient().when(fredApiProperties.getReadTimeout()).thenReturn(5000);

    fredApiClient = new FredApiClient(restTemplate, fredApiProperties);
  }

  @Test
  void testFetchLatestYieldCurve_Success() {
    // Arrange
    FredApiResponse mockResponse = createMockFredResponse("4.25");
    when(restTemplate.getForObject(anyString(), eq(FredApiResponse.class)))
      .thenReturn(mockResponse);

    // Act
    YieldCurveResponse result = fredApiClient.fetchLatestYieldCurve();

    // Assert
    assertNotNull(result);
    assertNotNull(result.getYields());
    assertTrue(result.getYields().size() > 0);
    assertEquals("FRED", result.getSource());
    assertEquals(LocalDate.now(), result.getDate());
    assertEquals(LocalDate.now(), result.getLastUpdated());
  }

  @Test
  void testFetchHistoricalYieldCurve_Success() {
    // Arrange
    LocalDate testDate = LocalDate.of(2024, 1, 15);
    FredApiResponse mockResponse = createMockFredResponse("4.15");
    when(restTemplate.getForObject(anyString(), eq(FredApiResponse.class)))
      .thenReturn(mockResponse);

    // Act
    YieldCurveResponse result = fredApiClient.fetchHistoricalYieldCurve(testDate);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getYields());
    assertEquals("FRED", result.getSource());
    assertEquals(testDate, result.getDate());
    assertEquals(LocalDate.now(), result.getLastUpdated());
  }

  @Test
  void testFetchHistoricalYieldCurve_InvalidDate() {
    // Arrange
    LocalDate futureDate = LocalDate.now().plusDays(1);

    // Act & Assert
    assertThrows(MarketDataException.class, () -> 
      fredApiClient.fetchHistoricalYieldCurve(futureDate));
  }

  @Test
  void testFetchYieldTimeSeries_Success() {
    // Arrange
    LocalDate startDate = LocalDate.of(2024, 1, 1);
    LocalDate endDate = LocalDate.of(2024, 1, 31);
    FredApiResponse mockResponse = createMockTimeSeriesResponse();
    when(restTemplate.getForObject(anyString(), eq(FredApiResponse.class)))
      .thenReturn(mockResponse);

    // Act
    List<MarketData> result = fredApiClient.fetchYieldTimeSeries("10Y", startDate, endDate);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(MarketData.DataType.YIELD_CURVE, result.get(0).getDataType());
    assertEquals("10Y", result.get(0).getTenor());
    assertEquals("FRED", result.get(0).getSource());
    assertEquals("EUR", result.get(0).getCurrency());
  }

  @Test
  void testFetchYieldTimeSeries_InvalidTenor() {
    // Act & Assert
    assertThrows(MarketDataException.class, () -> 
      fredApiClient.fetchYieldTimeSeries("INVALID", LocalDate.now().minusDays(1), LocalDate.now()));
  }

  @Test
  void testFetchYieldTimeSeries_InvalidDateRange() {
    // Arrange
    LocalDate startDate = LocalDate.now();
    LocalDate endDate = LocalDate.now().minusDays(1);

    // Act & Assert
    assertThrows(MarketDataException.class, () -> 
      fredApiClient.fetchYieldTimeSeries("10Y", startDate, endDate));
  }

  @Test
  void testFetchYieldCurvesForDates_Success() {
    // Arrange
    List<LocalDate> dates = List.of(
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 1, 2)
    );
    FredApiResponse mockResponse = createMockFredResponse("4.20");
    when(restTemplate.getForObject(anyString(), eq(FredApiResponse.class)))
      .thenReturn(mockResponse);

    // Act
    List<YieldCurveResponse> result = fredApiClient.fetchYieldCurvesForDates(dates);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("FRED", result.get(0).getSource());
  }

  @Test
  void testFetchYieldCurvesForDates_EmptyList() {
    // Act & Assert
    assertThrows(MarketDataException.class, () -> 
      fredApiClient.fetchYieldCurvesForDates(List.of()));
  }

  @Test
  void testValidateApiKey_MissingKey() {
    // Arrange - create a specific client with empty API key
    when(fredApiProperties.getApiKey()).thenReturn("");
    FredApiClient clientWithoutKey = new FredApiClient(restTemplate, fredApiProperties);

    // Act & Assert
    assertThrows(MarketDataException.class, () -> 
      clientWithoutKey.fetchLatestYieldCurve());
  }

  @Test
  void testYieldCurveResponse_HelperMethods() {
    // Arrange
    FredApiResponse mockResponse = createMockFredResponse("4.25");
    when(restTemplate.getForObject(anyString(), eq(FredApiResponse.class)))
      .thenReturn(mockResponse);

    // Act
    YieldCurveResponse result = fredApiClient.fetchLatestYieldCurve();

    // Assert
    assertNotNull(result);

    // Test helper methods if there are yields
    if (!result.getYields().isEmpty()) {
      String firstTenor = result.getYields().keySet().iterator().next();
      assertTrue(result.hasYieldForTenor(firstTenor));
      assertNotNull(result.getYieldForTenor(firstTenor));

      assertFalse(result.hasYieldForTenor("NONEXISTENT"));
      assertNull(result.getYieldForTenor("NONEXISTENT"));
    }
  }

  private FredApiResponse createMockFredResponse(String value) {
    FredApiResponse response = new FredApiResponse();
    response.setCount(1);

    FredApiResponse.FredObservation observation = new FredApiResponse.FredObservation();
    observation.setDate("2024-01-15");
    observation.setValue(value);
    observation.setRealtimeStart("2024-01-15");
    observation.setRealtimeEnd("2024-01-15");

    response.setObservations(List.of(observation));
    return response;
  }

  private FredApiResponse createMockTimeSeriesResponse() {
    FredApiResponse response = new FredApiResponse();
    response.setCount(2);

    FredApiResponse.FredObservation obs1 = new FredApiResponse.FredObservation();
    obs1.setDate("2024-01-01");
    obs1.setValue("4.20");
    obs1.setRealtimeStart("2024-01-01");
    obs1.setRealtimeEnd("2024-01-01");

    FredApiResponse.FredObservation obs2 = new FredApiResponse.FredObservation();
    obs2.setDate("2024-01-02");
    obs2.setValue("4.25");
    obs2.setRealtimeStart("2024-01-02");
    obs2.setRealtimeEnd("2024-01-02");

    response.setObservations(List.of(obs1, obs2));
    return response;
  }
}