package com.fixedincome.marketdata.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Static fallback market data for when external APIs are unavailable.
 * This data serves as reasonable defaults for valuation calculations.
 */
public final class FallbackMarketData {

  private FallbackMarketData() {} // Utility class

  // Credit spreads by rating (in basis points)
  public static final Map<String, BigDecimal> CREDIT_SPREADS = Map.of(
    "AAA", new BigDecimal("25"),
    "AA", new BigDecimal("40"),
    "A", new BigDecimal("75"),
    "BBB", new BigDecimal("150"),
    "BB", new BigDecimal("300"),
    "B", new BigDecimal("500"),
    "CCC", new BigDecimal("800"),
    "CC", new BigDecimal("1200"),
    "C", new BigDecimal("2000")
  );

  // Yield curves by region (yield percentage)
  public static final Map<String, Map<String, BigDecimal>> YIELD_CURVES;
  
  static {
    Map<String, Map<String, BigDecimal>> curves = new HashMap<>();
    
    // US Treasury yield curve
    Map<String, BigDecimal> usTreasury = new HashMap<>();
    usTreasury.put("1M", new BigDecimal("5.31"));
    usTreasury.put("3M", new BigDecimal("5.28"));
    usTreasury.put("6M", new BigDecimal("5.12"));
    usTreasury.put("1Y", new BigDecimal("4.80"));
    usTreasury.put("2Y", new BigDecimal("4.49"));
    usTreasury.put("3Y", new BigDecimal("4.25"));
    usTreasury.put("5Y", new BigDecimal("4.18"));
    usTreasury.put("7Y", new BigDecimal("4.20"));
    usTreasury.put("10Y", new BigDecimal("4.23"));
    usTreasury.put("20Y", new BigDecimal("4.47"));
    usTreasury.put("30Y", new BigDecimal("4.39"));
    curves.put("US", usTreasury);

    // Euro area yield curve
    Map<String, BigDecimal> euroArea = new HashMap<>();
    euroArea.put("1M", new BigDecimal("3.75"));
    euroArea.put("3M", new BigDecimal("3.72"));
    euroArea.put("6M", new BigDecimal("3.51"));
    euroArea.put("1Y", new BigDecimal("3.40"));
    euroArea.put("2Y", new BigDecimal("3.15"));
    euroArea.put("3Y", new BigDecimal("3.10"));
    euroArea.put("5Y", new BigDecimal("2.92"));
    euroArea.put("7Y", new BigDecimal("2.89"));
    euroArea.put("10Y", new BigDecimal("3.05"));
    euroArea.put("20Y", new BigDecimal("3.31"));
    euroArea.put("30Y", new BigDecimal("3.45"));
    curves.put("EUR", euroArea);

    // UK yield curve
    Map<String, BigDecimal> ukGilts = new HashMap<>();
    ukGilts.put("1M", new BigDecimal("5.15"));
    ukGilts.put("3M", new BigDecimal("5.05"));
    ukGilts.put("6M", new BigDecimal("4.95"));
    ukGilts.put("1Y", new BigDecimal("4.65"));
    ukGilts.put("2Y", new BigDecimal("4.35"));
    ukGilts.put("3Y", new BigDecimal("4.20"));
    ukGilts.put("5Y", new BigDecimal("4.10"));
    ukGilts.put("7Y", new BigDecimal("4.12"));
    ukGilts.put("10Y", new BigDecimal("4.15"));
    ukGilts.put("20Y", new BigDecimal("4.40"));
    ukGilts.put("30Y", new BigDecimal("4.35"));
    curves.put("UK", ukGilts);

    // Japan yield curve
    Map<String, BigDecimal> japanJgbs = new HashMap<>();
    japanJgbs.put("1M", new BigDecimal("0.08"));
    japanJgbs.put("3M", new BigDecimal("0.12"));
    japanJgbs.put("6M", new BigDecimal("0.15"));
    japanJgbs.put("1Y", new BigDecimal("0.20"));
    japanJgbs.put("2Y", new BigDecimal("0.28"));
    japanJgbs.put("3Y", new BigDecimal("0.35"));
    japanJgbs.put("5Y", new BigDecimal("0.59"));
    japanJgbs.put("7Y", new BigDecimal("0.75"));
    japanJgbs.put("10Y", new BigDecimal("0.95"));
    japanJgbs.put("20Y", new BigDecimal("1.70"));
    japanJgbs.put("30Y", new BigDecimal("1.90"));
    curves.put("JP", japanJgbs);
    
    YIELD_CURVES = Map.copyOf(curves);
  }

  // Central bank benchmark rates by region
  public static final Map<String, BigDecimal> BENCHMARK_RATES = Map.of(
    "US", new BigDecimal("5.25"),   // Fed Funds Rate
    "EUR", new BigDecimal("3.75"),  // ECB Deposit Rate
    "UK", new BigDecimal("5.00"),   // Bank of England
    "JP", new BigDecimal("0.10"),   // Bank of Japan
    "CA", new BigDecimal("4.50"),   // Bank of Canada
    "AU", new BigDecimal("4.10")    // Reserve Bank of Australia
  );

  // Inflation expectations by region (breakeven rates)
  public static final Map<String, Map<String, BigDecimal>> INFLATION_EXPECTATIONS;
  
  static {
    Map<String, Map<String, BigDecimal>> expectations = new HashMap<>();
    
    Map<String, BigDecimal> usInflation = new HashMap<>();
    usInflation.put("2Y", new BigDecimal("2.5"));
    usInflation.put("5Y", new BigDecimal("2.3"));
    usInflation.put("10Y", new BigDecimal("2.2"));
    usInflation.put("30Y", new BigDecimal("2.1"));
    expectations.put("US", usInflation);

    Map<String, BigDecimal> eurInflation = new HashMap<>();
    eurInflation.put("2Y", new BigDecimal("2.1"));
    eurInflation.put("5Y", new BigDecimal("2.0"));
    eurInflation.put("10Y", new BigDecimal("1.9"));
    eurInflation.put("30Y", new BigDecimal("1.8"));
    expectations.put("EUR", eurInflation);

    Map<String, BigDecimal> ukInflation = new HashMap<>();
    ukInflation.put("2Y", new BigDecimal("3.2"));
    ukInflation.put("5Y", new BigDecimal("3.0"));
    ukInflation.put("10Y", new BigDecimal("2.8"));
    ukInflation.put("30Y", new BigDecimal("2.7"));
    expectations.put("UK", ukInflation);
    
    INFLATION_EXPECTATIONS = Map.copyOf(expectations);
  }

  // Liquidity premiums by instrument type (in basis points)
  public static final Map<String, BigDecimal> LIQUIDITY_PREMIUMS = Map.of(
    "GOVERNMENT", new BigDecimal("0"),
    "CORPORATE", new BigDecimal("15"),
    "MUNICIPAL", new BigDecimal("25"),
    "HIGH_YIELD", new BigDecimal("50"),
    "EMERGING_MARKET", new BigDecimal("75")
  );
}
