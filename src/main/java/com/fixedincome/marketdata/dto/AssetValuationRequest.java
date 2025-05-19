package com.fixedincome.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetValuationRequest {

  @NotNull(message = "Asset ID is required")
  private UUID assetId;
  
  private String isin;
  
  private String cusip;
  
  @NotNull(message = "Valuation date is required")
  private LocalDate valuationDate;
  
  private String valuationMethod;
  
  // Additional optional parameters that might override asset defaults
  private BigDecimal customDiscountRate;
  
  private String customYieldCurve;
  
  private String customCreditSpread;
}
