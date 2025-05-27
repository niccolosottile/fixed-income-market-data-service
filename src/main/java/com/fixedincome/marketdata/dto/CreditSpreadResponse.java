package com.fixedincome.marketdata.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CreditSpreadResponse {
  
  private Map<String, BigDecimal> spreads;
  private LocalDateTime timestamp;
  private String source;
  private String currency;
  
  // Credit rating categories with their spreads (in basis points)
  @Data
  public static class CreditSpread {
    private String rating;
    private BigDecimal spread; // in basis points
    private String sector;
    private Integer maturityBucket; // years
    private LocalDateTime lastUpdated;
  }
}
