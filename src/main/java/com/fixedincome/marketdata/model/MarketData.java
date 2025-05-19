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
@Table(name = "market_data")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class MarketData extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private DataType dataType;
  
  @Column(nullable = false)
  private String dataKey;
  
  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal dataValue;
  
  @Column(nullable = false)
  private LocalDate dataDate;
  
  @Column(nullable = false)
  private String source;
  
  @Column
  private String currency;
  
  @Column
  private String tenor;
  
  // Create a composite unique constraint
  @Table(name = "market_data", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"data_type", "data_key", "data_date", "source"})
  })
  
  public enum DataType {
    YIELD_CURVE,
    CREDIT_SPREAD,
    SWAP_RATE,
    BENCHMARK_RATE,
    LIBOR,
    SOFR,
    FX_RATE,
    BOND_PRICE
  }
}
