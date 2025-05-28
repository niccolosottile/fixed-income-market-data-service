package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.dto.YieldCurveResponse;
import com.fixedincome.marketdata.model.MarketData;
import com.fixedincome.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for database operations related to market data.
 * Handles storing and retrieving market data from the database.
 */
@Service
@Transactional
public class MarketDataDatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataDatabaseService.class);

  private final MarketDataRepository marketDataRepository;

  public MarketDataDatabaseService(MarketDataRepository marketDataRepository) {
    this.marketDataRepository = marketDataRepository;
  }

  // ===== YIELD CURVE DATABASE OPERATIONS =====

  /**
   * Store yield curve data in database
   */
  public void storeYieldCurve(YieldCurveResponse yieldCurve) {
    try {
      List<MarketData> marketDataList = yieldCurve.getYields().entrySet().stream()
        .map(entry -> MarketData.builder()
          .dataType(MarketData.DataType.YIELD_CURVE)
          .dataKey(entry.getKey()) // tenor
          .dataValue(entry.getValue())
          .dataDate(yieldCurve.getDate())
          .source(yieldCurve.getSource())
          .tenor(entry.getKey())
          .build())
        .toList();

      marketDataRepository.saveAll(marketDataList);
      logger.debug("Stored yield curve with {} data points for date {}", 
        marketDataList.size(), yieldCurve.getDate());
    } catch (Exception e) {
      logger.error("Failed to store yield curve data: {}", e.getMessage(), e);
    }
  }

  /**
   * Retrieve latest yield curve from database
   */
  public Optional<YieldCurveResponse> getLatestYieldCurve() {
    try {
      List<MarketData> latestData = marketDataRepository.findLatestByDataTypeAsOfDate(
        MarketData.DataType.YIELD_CURVE, LocalDate.now());
      
      return buildYieldCurveFromMarketData(latestData);
    } catch (Exception e) {
      logger.error("Failed to retrieve latest yield curve from database: {}", e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Retrieve historical yield curve for specific date
   */
  public Optional<YieldCurveResponse> getHistoricalYieldCurve(LocalDate date) {
    try {
      List<MarketData> historicalData = marketDataRepository.findLatestByDataTypeAsOfDate(
        MarketData.DataType.YIELD_CURVE, date);
      
      return buildYieldCurveFromMarketData(historicalData);
    } catch (Exception e) {
      logger.error("Failed to retrieve historical yield curve for date {}: {}", date, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Retrieve yield curves for multiple dates
   */
  public List<YieldCurveResponse> getYieldCurvesForDates(List<LocalDate> dates) {
    return dates.stream()
      .map(this::getHistoricalYieldCurve)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .toList();
  }

  /**
   * Get time series data for specific tenor
   */
  public List<MarketData> getYieldTimeSeries(String tenor, LocalDate startDate, LocalDate endDate) {
    try {
      return marketDataRepository.findByDataTypeAndDataKeyAndDataDateBetweenOrderByDataDateAsc(
        MarketData.DataType.YIELD_CURVE, tenor, startDate, endDate);
    } catch (Exception e) {
      logger.error("Failed to retrieve time series for tenor {}: {}", tenor, e.getMessage());
      return List.of();
    }
  }

  // ===== CREDIT AND SPREAD DATABASE OPERATIONS =====

  /**
   * Store credit spreads in database
   */
  public void storeCreditSpreads(Map<String, BigDecimal> spreads, String source) {
    try {
      List<MarketData> marketDataList = spreads.entrySet().stream()
        .map(entry -> MarketData.builder()
          .dataType(MarketData.DataType.CREDIT_SPREAD)
          .dataKey(entry.getKey())
          .dataValue(entry.getValue())
          .dataDate(LocalDate.now())
          .source(source)
          .build())
        .toList();

      marketDataRepository.saveAll(marketDataList);
      logger.debug("Stored {} credit spreads", marketDataList.size());
    } catch (Exception e) {
      logger.error("Failed to store credit spreads: {}", e.getMessage(), e);
    }
  }

  /**
   * Retrieve latest credit spreads from database
   */
  public Optional<Map<String, BigDecimal>> getLatestCreditSpreads() {
    try {
      List<MarketData> latestData = marketDataRepository.findLatestByDataTypeAsOfDate(
        MarketData.DataType.CREDIT_SPREAD, LocalDate.now());
      
      if (latestData.isEmpty()) {
        return Optional.empty();
      }

      Map<String, BigDecimal> spreads = latestData.stream()
        .collect(Collectors.toMap(
          MarketData::getDataKey,
          MarketData::getDataValue,
          (existing, replacement) -> replacement));

      return Optional.of(spreads);
    } catch (Exception e) {
      logger.error("Failed to retrieve credit spreads from database: {}", e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Store benchmark rates in database
   */
  public void storeBenchmarkRates(Map<String, BigDecimal> rates, String source) {
    try {
      List<MarketData> marketDataList = rates.entrySet().stream()
        .map(entry -> MarketData.builder()
          .dataType(MarketData.DataType.BENCHMARK_RATE)
          .dataKey(entry.getKey())
          .dataValue(entry.getValue())
          .dataDate(LocalDate.now())
          .source(source)
          .build())
        .toList();

      marketDataRepository.saveAll(marketDataList);
      logger.debug("Stored {} benchmark rates", marketDataList.size());
    } catch (Exception e) {
      logger.error("Failed to store benchmark rates: {}", e.getMessage(), e);
    }
  }

  /**
   * Retrieve latest benchmark rates from database
   */
  public Optional<Map<String, BigDecimal>> getLatestBenchmarkRates() {
    try {
      List<MarketData> latestData = marketDataRepository.findLatestByDataTypeAsOfDate(
        MarketData.DataType.BENCHMARK_RATE, LocalDate.now());
      
      if (latestData.isEmpty()) {
        return Optional.empty();
      }

      Map<String, BigDecimal> rates = latestData.stream()
        .collect(Collectors.toMap(
          MarketData::getDataKey,
          MarketData::getDataValue,
          (existing, replacement) -> replacement));

      return Optional.of(rates);
    } catch (Exception e) {
      logger.error("Failed to retrieve benchmark rates from database: {}", e.getMessage());
      return Optional.empty();
    }
  }

  // ===== UTILITY METHODS =====

  /**
   * Get specific yield for tenor from database
   */
  public Optional<BigDecimal> getYieldForTenor(String tenor, LocalDate date) {
    try {
      Optional<MarketData> data = marketDataRepository.findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
        MarketData.DataType.YIELD_CURVE, tenor, date);
      
      return data.map(MarketData::getDataValue);
    } catch (Exception e) {
      logger.error("Failed to retrieve yield for tenor {} and date {}: {}", tenor, date, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Get specific credit spread for rating from database
   */
  public Optional<BigDecimal> getCreditSpreadForRating(String rating) {
    try {
      Optional<MarketData> data = marketDataRepository.findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
        MarketData.DataType.CREDIT_SPREAD, rating, LocalDate.now());
      
      return data.map(MarketData::getDataValue);
    } catch (Exception e) {
      logger.error("Failed to retrieve credit spread for rating {}: {}", rating, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Get specific benchmark rate for region from database
   */
  public Optional<BigDecimal> getBenchmarkRateForRegion(String region) {
    try {
      Optional<MarketData> data = marketDataRepository.findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
        MarketData.DataType.BENCHMARK_RATE, region, LocalDate.now());
      
      return data.map(MarketData::getDataValue);
    } catch (Exception e) {
      logger.error("Failed to retrieve benchmark rate for region {}: {}", region, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Clean old data from database
   */
  public void cleanOldData(LocalDate beforeDate) {
    try {
      marketDataRepository.deleteByDataDateBefore(beforeDate);
      logger.info("Cleaned market data before date {}", beforeDate);
    } catch (Exception e) {
      logger.error("Failed to clean old market data: {}", e.getMessage(), e);
    }
  }

  // ===== PRIVATE HELPER METHODS =====

  private Optional<YieldCurveResponse> buildYieldCurveFromMarketData(List<MarketData> marketDataList) {
    if (marketDataList.isEmpty()) {
      return Optional.empty();
    }

    Map<String, BigDecimal> yields = marketDataList.stream()
      .collect(Collectors.toMap(
        MarketData::getDataKey,
        MarketData::getDataValue,
        (existing, replacement) -> replacement));

    MarketData firstRecord = marketDataList.get(0);
    
    return Optional.of(YieldCurveResponse.builder()
      .date(firstRecord.getDataDate())
      .source(firstRecord.getSource())
      .yields(yields)
      .lastUpdated(LocalDate.now())
      .build());
  }
}