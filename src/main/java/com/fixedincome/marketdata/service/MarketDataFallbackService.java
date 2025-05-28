package com.fixedincome.marketdata.service;

import com.fixedincome.marketdata.config.FallbackMarketData;
import com.fixedincome.marketdata.dto.YieldCurveResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that provides fallback market data when external providers
 * and database are unavailable.
 */
@Service
public class MarketDataFallbackService {

  private static final Logger logger = LoggerFactory.getLogger(MarketDataFallbackService.class);

  /**
   * Create fallback yield curve for current date
   */
  public YieldCurveResponse createFallbackYieldCurve() {
    return createFallbackYieldCurve(LocalDate.now());
  }

  /**
   * Create fallback yield curve for specific date
   */
  public YieldCurveResponse createFallbackYieldCurve(LocalDate date) {
    return getFallbackYieldCurve("EUR", date);
  }

  /**
   * Get fallback yield curve for specific region
   */
  public YieldCurveResponse getFallbackYieldCurve(String region) {
    return getFallbackYieldCurve(region, LocalDate.now());
  }

  /**
   * Get fallback yield curve for specific region and date
   */
  public YieldCurveResponse getFallbackYieldCurve(String region, LocalDate date) {
    logger.info("Using fallback yield curve data for region {} and date {}", region, date);
    
    String safeRegion = (region != null) ? region.toUpperCase() : "EUR";
    Map<String, BigDecimal> regionYields = FallbackMarketData.YIELD_CURVES.get(safeRegion);
    
    // Default to EUR if region not found
    if (regionYields == null) {
      regionYields = FallbackMarketData.YIELD_CURVES.get("EUR");
    }
    
    return YieldCurveResponse.builder()
      .date(date)
      .source("FALLBACK")
      .yields(regionYields)
      .lastUpdated(LocalDate.now())
      .build();
  }

  /**
   * Create fallback yield curves for multiple dates
   */
  public List<YieldCurveResponse> createFallbackYieldCurves(List<LocalDate> dates) {
    return getFallbackYieldCurvesForDates(dates, "EUR");
  }

  /**
   * Get fallback yield curves for multiple dates and region
   */
  public List<YieldCurveResponse> getFallbackYieldCurvesForDates(List<LocalDate> dates, String region) {
    if (dates == null || dates.isEmpty()) {
      return List.of();
    }
    
    logger.info("Using fallback yield curve data for {} dates in region {}", dates.size(), region);
    
    return dates.stream()
      .map(date -> getFallbackYieldCurve(region, date))
      .collect(Collectors.toList());
  }

  /**
   * Get fallback credit spreads
   */
  public Map<String, BigDecimal> getFallbackCreditSpreads() {
    logger.info("Using fallback credit spread data");
    return FallbackMarketData.CREDIT_SPREADS;
  }

  /**
   * Get fallback inflation expectations for region
   */
  public Map<String, BigDecimal> getFallbackInflationExpectations(String region) {
    logger.info("Using fallback inflation expectations data for region {}", region);
    
    String safeRegion = (region != null) ? region.toUpperCase() : "EUR";
    Map<String, BigDecimal> regionExpectations = FallbackMarketData.INFLATION_EXPECTATIONS.get(safeRegion);
    
    // Default to EUR if region not found
    if (regionExpectations == null) {
      regionExpectations = FallbackMarketData.INFLATION_EXPECTATIONS.get("EUR");
    }
    
    return regionExpectations;
  }

  /**
   * Get fallback benchmark rates
   */
  public Map<String, BigDecimal> getFallbackBenchmarkRates() {
    logger.info("Using fallback benchmark rate data");
    return FallbackMarketData.BENCHMARK_RATES;
  }

  /**
   * Get fallback liquidity premiums
   */
  public Map<String, BigDecimal> getFallbackLiquidityPremiums() {
    logger.info("Using fallback liquidity premium data");
    return FallbackMarketData.LIQUIDITY_PREMIUMS;
  }

  /**
   * Get fallback yield for specific tenor and region
   */
  public BigDecimal getYieldForTenor(String tenor, String region) {
    return getFallbackYieldForTenor(tenor, region);
  }

  /**
   * Get fallback yield for specific tenor and region
   */
  public BigDecimal getFallbackYieldForTenor(String tenor, String region) {
    String safeRegion = (region != null) ? region.toUpperCase() : "EUR";
    String safeTenor = (tenor != null) ? tenor : "10Y";
    
    Map<String, BigDecimal> regionCurve = FallbackMarketData.YIELD_CURVES.get(safeRegion);
    if (regionCurve == null) {
      regionCurve = FallbackMarketData.YIELD_CURVES.get("EUR"); // Default to EUR
    }
    
    BigDecimal yield = regionCurve.get(safeTenor);
    if (yield == null) {
      // If tenor not found, return 30Y rate as a reasonable default
      yield = regionCurve.get("30Y");
      if (yield == null) {
        yield = new BigDecimal("4.00"); // Fallback to 4%
      }
    }
    
    return yield;
  }

  /**
   * Get fallback credit spread for rating
   */
  public BigDecimal getCreditSpreadForRating(String rating) {
    return getFallbackCreditSpreadForRating(rating);
  }

  /**
   * Get fallback credit spread for rating
   */
  public BigDecimal getFallbackCreditSpreadForRating(String rating) {
    String safeRating = (rating != null) ? rating.toUpperCase() : "BBB";
    
    BigDecimal spread = FallbackMarketData.CREDIT_SPREADS.get(safeRating);
    if (spread == null) {
      // Default to BBB spread if rating not found
      spread = FallbackMarketData.CREDIT_SPREADS.get("BBB");
      if (spread == null) {
        spread = new BigDecimal("150"); // 150 basis points default
      }
    }
    
    return spread;
  }

  /**
   * Get fallback benchmark rate for region
   */
  public BigDecimal getBenchmarkRateForRegion(String region) {
    return getFallbackBenchmarkRateForRegion(region);
  }

  /**
   * Get fallback benchmark rate for region
   */
  public BigDecimal getFallbackBenchmarkRateForRegion(String region) {
    String safeRegion = (region != null) ? region.toUpperCase() : "EUR";
    
    BigDecimal rate = FallbackMarketData.BENCHMARK_RATES.get(safeRegion);
    if (rate == null) {
      // Default to EUR rate if region not found
      rate = FallbackMarketData.BENCHMARK_RATES.get("EUR");
      if (rate == null) {
        rate = new BigDecimal("3.75"); // 3.75% default
      }
    }
    
    return rate;
  }

  /**
   * Get fallback sector credit data with sector-specific adjustments
   */
  public Map<String, BigDecimal> getFallbackSectorCreditData(String sector) {
    logger.info("Using fallback credit spreads for sector {}", sector);
    
    if (sector == null) {
      return FallbackMarketData.CREDIT_SPREADS;
    }
    
    // Apply sector-specific adjustments
    String safeSector = sector.toUpperCase();
    Map<String, BigDecimal> baseSpreads = FallbackMarketData.CREDIT_SPREADS;
    
    return baseSpreads.entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> applySectorAdjustment(entry.getValue(), safeSector)
      ));
  }

  /**
   * Apply sector-specific adjustments to credit spreads
   */
  private BigDecimal applySectorAdjustment(BigDecimal baseSpread, String sector) {
    BigDecimal adjustmentFactor;
    
    switch (sector) {
      case "TECH":
        adjustmentFactor = new BigDecimal("0.85"); // Tech sector: -15%
        break;
      case "ENERGY":
        adjustmentFactor = new BigDecimal("1.20"); // Energy sector: +20%
        break;
      case "UTILITIES":
        adjustmentFactor = new BigDecimal("0.90"); // Utilities: -10%
        break;
      case "FINANCIALS":
        adjustmentFactor = new BigDecimal("1.10"); // Financials: +10%
        break;
      case "HEALTHCARE":
        adjustmentFactor = new BigDecimal("0.95"); // Healthcare: -5%
        break;
      default:
        adjustmentFactor = BigDecimal.ONE; // No adjustment for unknown sectors
        break;
    }
    
    return baseSpread.multiply(adjustmentFactor).setScale(0, RoundingMode.HALF_UP);
  }
}