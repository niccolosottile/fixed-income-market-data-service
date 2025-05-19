package com.fixedincome.marketdata.repository;

import com.fixedincome.marketdata.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, UUID> {
    
  List<MarketData> findByDataType(MarketData.DataType dataType);
  
  List<MarketData> findByDataTypeAndDataDate(MarketData.DataType dataType, LocalDate dataDate);
  
  Optional<MarketData> findTopByDataTypeAndDataKeyAndDataDateLessThanEqualOrderByDataDateDesc(
    MarketData.DataType dataType, String dataKey, LocalDate dataDate);
  
  List<MarketData> findByDataTypeAndDataKeyAndDataDateBetweenOrderByDataDateAsc(
    MarketData.DataType dataType, String dataKey, LocalDate startDate, LocalDate endDate);
  
  @Query("SELECT m FROM MarketData m WHERE m.dataType = :dataType AND m.dataDate = " +
    "(SELECT MAX(m2.dataDate) FROM MarketData m2 WHERE m2.dataType = :dataType AND m2.dataDate <= :asOfDate)")
  List<MarketData> findLatestByDataTypeAsOfDate(MarketData.DataType dataType, LocalDate asOfDate);
  
  void deleteByDataDateBefore(LocalDate date);
}
