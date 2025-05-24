package com.fixedincome.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldCurveResponse {
    
  private LocalDate date;
  private String source;
  private Map<String, BigDecimal> yields;
  private LocalDate lastUpdated;
  
  // Helper methods
  public BigDecimal getYieldForTenor(String tenor) {
    return yields != null ? yields.get(tenor) : null;
  }
  
  public boolean hasYieldForTenor(String tenor) {
    return yields != null && yields.containsKey(tenor) && yields.get(tenor) != null;
  }
}