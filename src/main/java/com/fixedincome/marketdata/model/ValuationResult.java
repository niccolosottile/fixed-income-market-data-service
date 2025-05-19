package com.fixedincome.marketdata.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "valuation_results")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class ValuationResult extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", nullable = false)
  private FixedIncomeAsset asset;
  
  @Column(nullable = false)
  private LocalDate valuationDate;
  
  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal marketValue;
  
  @Column(precision = 19, scale = 6)
  private BigDecimal yieldToMaturity;
  
  @Column(precision = 19, scale = 6)
  private BigDecimal modifiedDuration;
  
  @Column(precision = 19, scale = 6)
  private BigDecimal macaulayDuration;
  
  @Column(precision = 19, scale = 6)
  private BigDecimal convexity;
  
  @Column
  private String valuationMethod;
  
  @Column
  private String currencyCode;
  
  @Column
  private String notes;
}
