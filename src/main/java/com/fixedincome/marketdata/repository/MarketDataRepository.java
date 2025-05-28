package com.fixedincome.marketdata.repository;

import com.fixedincome.marketdata.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
  List<MarketData> findLatestByDataTypeAsOfDate(@Param("dataType") MarketData.DataType dataType, @Param("asOfDate") LocalDate asOfDate);
  
  @Query("SELECT m FROM MarketData m WHERE m.dataType = :dataType AND m.dataKey = :dataKey AND m.dataDate = " +
    "(SELECT MAX(m2.dataDate) FROM MarketData m2 WHERE m2.dataType = :dataType AND m2.dataKey = :dataKey AND m2.dataDate <= :asOfDate)")
  Optional<MarketData> findLatestByDataTypeAndKeyAsOfDate(
    @Param("dataType") MarketData.DataType dataType, 
    @Param("dataKey") String dataKey, 
    @Param("asOfDate") LocalDate asOfDate);
  
  @Query("SELECT DISTINCT m.dataKey FROM MarketData m WHERE m.dataType = :dataType ORDER BY m.dataKey")
  List<String> findDistinctDataKeysByDataType(@Param("dataType") MarketData.DataType dataType);
  
  @Query("SELECT DISTINCT m.source FROM MarketData m WHERE m.dataType = :dataType")
  List<String> findDistinctSourcesByDataType(@Param("dataType") MarketData.DataType dataType);
  
  @Query("SELECT m FROM MarketData m WHERE m.dataType = :dataType AND m.source = :source AND m.dataDate = " +
    "(SELECT MAX(m2.dataDate) FROM MarketData m2 WHERE m2.dataType = :dataType AND m2.source = :source AND m2.dataDate <= :asOfDate)")
  List<MarketData> findLatestByDataTypeAndSourceAsOfDate(
    @Param("dataType") MarketData.DataType dataType, 
    @Param("source") String source, 
    @Param("asOfDate") LocalDate asOfDate);
  
  @Query("SELECT COUNT(m) FROM MarketData m WHERE m.dataType = :dataType AND m.dataDate = :date")
  long countByDataTypeAndDate(@Param("dataType") MarketData.DataType dataType, @Param("date") LocalDate date);
  
  @Query("SELECT m FROM MarketData m WHERE m.dataType = :dataType AND m.dataDate BETWEEN :startDate AND :endDate ORDER BY m.dataDate DESC, m.dataKey ASC")
  List<MarketData> findByDataTypeAndDateRange(
    @Param("dataType") MarketData.DataType dataType, 
    @Param("startDate") LocalDate startDate, 
    @Param("endDate") LocalDate endDate);
  
  @Query("SELECT m FROM MarketData m WHERE m.dataType = :dataType AND m.dataKey IN :keys AND m.dataDate = " +
    "(SELECT MAX(m2.dataDate) FROM MarketData m2 WHERE m2.dataType = :dataType AND m2.dataKey = m.dataKey AND m2.dataDate <= :asOfDate)")
  List<MarketData> findLatestByDataTypeAndKeysAsOfDate(
    @Param("dataType") MarketData.DataType dataType, 
    @Param("keys") List<String> keys, 
    @Param("asOfDate") LocalDate asOfDate);
  
  void deleteByDataDateBefore(LocalDate date);
  
  void deleteByDataTypeAndDataDateBefore(MarketData.DataType dataType, LocalDate date);
}
