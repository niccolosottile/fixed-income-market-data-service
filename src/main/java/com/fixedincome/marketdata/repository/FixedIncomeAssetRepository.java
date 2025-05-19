package com.fixedincome.marketdata.repository;

import com.fixedincome.marketdata.model.FixedIncomeAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FixedIncomeAssetRepository extends JpaRepository<FixedIncomeAsset, UUID> {
    
  Optional<FixedIncomeAsset> findByIsin(String isin);
  
  Optional<FixedIncomeAsset> findByCusip(String cusip);
  
  List<FixedIncomeAsset> findByIssuerName(String issuerName);
  
  List<FixedIncomeAsset> findByAssetType(FixedIncomeAsset.AssetType assetType);
  
  List<FixedIncomeAsset> findByCreditRating(String creditRating);
  
  List<FixedIncomeAsset> findByMaturityDateBetween(LocalDate startDate, LocalDate endDate);
}
