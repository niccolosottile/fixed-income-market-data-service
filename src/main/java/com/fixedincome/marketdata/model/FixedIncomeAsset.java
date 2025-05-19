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
@Table(name = "fixed_income_assets")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class FixedIncomeAsset extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  
  // Asset identifiers
  @Column(unique = true)
  private String isin;
  
  @Column
  private String cusip;
  
  // Basic bond details
  @Column(nullable = false)
  private String issuerName;
  
  @Column(nullable = false)
  private String assetName;
  
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AssetType assetType;
  
  // Financial details
  @Column(nullable = false)
  private BigDecimal parValue;
  
  @Column(nullable = false)
  private BigDecimal couponRate;
  
  @Column(nullable = false)
  private String currency;
  
  @Column(nullable = false)
  private LocalDate issueDate;
  
  @Column(nullable = false)
  private LocalDate maturityDate;
  
  // Credit information
  @Column
  private String creditRating;
  
  @Column
  private String ratingAgency;
  
  // Payment details
  @Column
  @Enumerated(EnumType.STRING)
  private PaymentFrequency paymentFrequency;
  
  @Column
  @Enumerated(EnumType.STRING)
  private DayCountConvention dayCountConvention;
  
  // Additional features
  @Column
  private boolean callable;
  
  @Column
  private boolean puttable;
  
  @Column
  private boolean convertible;
  
  // Enumerated types
  public enum AssetType {
    GOVERNMENT_BOND,
    CORPORATE_BOND,
    MUNICIPAL_BOND,
    TREASURY_BILL,
    CERTIFICATE_OF_DEPOSIT,
    COMMERCIAL_PAPER
  }
  
  public enum PaymentFrequency {
    ANNUAL,
    SEMI_ANNUAL,
    QUARTERLY,
    MONTHLY,
    ZERO_COUPON
  }
  
  public enum DayCountConvention {
    THIRTY_360,
    ACTUAL_360,
    ACTUAL_365,
    ACTUAL_ACTUAL
  }
}
