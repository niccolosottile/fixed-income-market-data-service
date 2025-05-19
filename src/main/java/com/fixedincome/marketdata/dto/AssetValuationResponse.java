package com.fixedincome.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetValuationResponse {

  private UUID assetId;
  
  private String assetName;
  
  private String isin;
  
  private String cusip;
  
  private LocalDate valuationDate;
  
  private BigDecimal marketValue;
  
  private BigDecimal parValue;
  
  private BigDecimal marketValuePercentage;
  
  private BigDecimal yieldToMaturity;
  
  private BigDecimal modifiedDuration;
  
  private BigDecimal macaulayDuration;
  
  private BigDecimal convexity;
  
  private String currency;
  
  private String valuationMethod;
  
  private LocalDate calculationTimestamp;
}
