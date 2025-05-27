package com.fixedincome.marketdata.service.integration;

import com.fixedincome.marketdata.exception.MarketDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class providing common validation and utility methods for all market data providers.
 * Concrete implementations must extend this class and implement the abstract methods.
 */
public abstract class AbstractMarketDataProvider implements MarketDataProvider {
  
  protected static final Logger logger = LoggerFactory.getLogger(AbstractMarketDataProvider.class);
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  
  // ===== COMMON VALIDATION METHODS =====
  
  /**
   * Validates that a tenor is not null/empty and is supported by this provider
   * @param tenor The tenor to validate
   * @throws MarketDataException if tenor is invalid
   */
  protected void validateTenor(String tenor) {
    if (tenor == null || tenor.trim().isEmpty()) {
      throw new MarketDataException("Tenor cannot be null or empty");
    }
    if (!getSupportedTenors().contains(tenor)) {
      throw new MarketDataException("Invalid tenor: " + tenor + ". Supported tenors: " + 
        String.join(", ", getSupportedTenors()));
    }
  }
  
  /**
   * Validates that a date is not null and not in the future
   * @param date The date to validate
   * @throws MarketDataException if date is invalid
   */
  protected void validateDate(LocalDate date) {
    if (date == null) {
      throw new MarketDataException("Date cannot be null");
    }
    if (date.isAfter(LocalDate.now())) {
      throw new MarketDataException("Date cannot be in the future");
    }
  }
  
  /**
   * Validates a date range (start and end dates)
   * @param startDate The start date
   * @param endDate The end date
   * @throws MarketDataException if date range is invalid
   */
  protected void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null) {
      throw new MarketDataException("Start date and end date cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new MarketDataException("Start date cannot be after end date");
    }
    if (startDate.isAfter(LocalDate.now())) {
      throw new MarketDataException("Start date cannot be in the future");
    }
  }
  
  // ===== COMMON UTILITY METHODS =====
  
  /**
   * Parses a string yield value to BigDecimal, handling common edge cases
   * @param value The string value to parse
   * @return BigDecimal yield value, or null if unparseable
   */
  protected BigDecimal parseYieldValue(String value) {
    if (value == null || value.trim().isEmpty() || ".".equals(value)) {
      return null;
    }
    
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      logger.warn("Unable to parse yield value: {}", value);
      return null;
    }
  }
  
  /**
   * Gets volatility factor for a given tenor (used in synthetic data generation)
   * Short-term rates tend to be more volatile than long-term rates
   * @param tenor The tenor
   * @return Volatility factor (higher = more volatile)
   */
  protected int getVolatilityFactor(String tenor) {
    // Short-term rates tend to be more volatile
    if (tenor.endsWith("M")) {
      return 8;  // Higher volatility for monthly tenors
    } else if (tenor.equals("1Y") || tenor.equals("2Y")) {
      return 6;  // Medium volatility for short tenors
    } else if (tenor.equals("5Y") || tenor.equals("7Y")) {
      return 5;  // Medium volatility for medium tenors
    } else {
      return 4;  // Lower volatility for long tenors
    }
  }
  
  // ===== ABSTRACT METHODS FOR PROVIDER-SPECIFIC LOGIC =====
  
  /**
   * Validates the API configuration for this specific provider
   * Each provider may have different configuration requirements (API keys, URLs, etc.)
   * @throws MarketDataException if configuration is invalid
   */
  protected abstract void validateApiConfiguration();
  
  /**
   * Gets the mapping of tenors to provider-specific series identifiers
   * @return Map of tenor to series ID (e.g., "10Y" -> "DGS10" for FRED)
   */
  protected abstract Map<String, String> getYieldCurveSeriesMapping();
  
  // ===== DEFAULT IMPLEMENTATIONS FOR UNSUPPORTED OPERATIONS =====
  
  /**
   * Default implementation throws UnsupportedOperationException.
   * Providers that support credit spreads should override this method.
   */
  @Override
  public Map<String, BigDecimal> fetchCreditSpreads() {
    throw new UnsupportedOperationException(getProviderName() + " does not provide credit spreads data");
  }
  
  /**
   * Default implementation throws UnsupportedOperationException.
   * Providers that support inflation expectations should override this method.
   */
  @Override
  public Map<String, BigDecimal> fetchInflationExpectations(String region) {
    throw new UnsupportedOperationException(getProviderName() + " does not provide inflation expectations data");
  }
  
  /**
   * Default implementation throws UnsupportedOperationException.
   * Providers that support benchmark rates should override this method.
   */
  @Override
  public Map<String, BigDecimal> fetchBenchmarkRates() {
    throw new UnsupportedOperationException(getProviderName() + " does not provide benchmark rates data");
  }
  
  /**
   * Default implementation throws UnsupportedOperationException.
   * Providers that support sector credit data should override this method.
   */
  @Override
  public Map<String, BigDecimal> fetchSectorCreditData(String sector) {
    throw new UnsupportedOperationException(getProviderName() + " does not provide sector credit data");
  }
  
  /**
   * Default implementation throws UnsupportedOperationException.
   * Providers that support liquidity premiums should override this method.
   */
  @Override
  public Map<String, BigDecimal> fetchLiquidityPremiums() {
    throw new UnsupportedOperationException(getProviderName() + " does not provide liquidity premiums data");
  }
  
  /**
   * Default implementation returns supported tenors from the yield curve series mapping
   */
  @Override
  public Set<String> getSupportedTenors() {
    return getYieldCurveSeriesMapping().keySet();
  }
}
