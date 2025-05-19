package com.fixedincome.marketdata.repository;

import com.fixedincome.marketdata.model.FixedIncomeAsset;
import com.fixedincome.marketdata.model.ValuationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ValuationResultRepository extends JpaRepository<ValuationResult, UUID> {
    
  List<ValuationResult> findByAssetOrderByValuationDateDesc(FixedIncomeAsset asset);
  
  List<ValuationResult> findByAssetAndValuationDateBetweenOrderByValuationDateAsc(
    FixedIncomeAsset asset, LocalDate startDate, LocalDate endDate);
  
  Optional<ValuationResult> findTopByAssetOrderByValuationDateDesc(FixedIncomeAsset asset);
  
  @Query("SELECT v FROM ValuationResult v WHERE v.asset.id = :assetId AND v.valuationDate = " +
    "(SELECT MAX(v2.valuationDate) FROM ValuationResult v2 WHERE v2.asset.id = :assetId AND v2.valuationDate <= :asOfDate)")
  Optional<ValuationResult> findLatestValuationForAssetAsOfDate(UUID assetId, LocalDate asOfDate);
  
  void deleteByValuationDateBefore(LocalDate date);
}
