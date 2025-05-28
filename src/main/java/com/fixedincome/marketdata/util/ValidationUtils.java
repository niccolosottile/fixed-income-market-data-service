package com.fixedincome.marketdata.util;

import com.fixedincome.marketdata.exception.DataValidationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Utility class for validating market data service inputs
 */
public class ValidationUtils {
  
  private static final Set<String> VALID_TENORS = Set.of(
    "1M", "3M", "6M", "1Y", "2Y", "3Y", "5Y", "7Y", "10Y", "20Y", "30Y"
  );
  
  private static final Set<String> VALID_REGIONS = Set.of(
    "US", "EUR", "UK", "JP", "CA", "AU"
  );
  
  private static final Set<String> VALID_RATINGS = Set.of(
    "AAA", "AA+", "AA", "AA-", "A+", "A", "A-", "BBB+", "BBB", "BBB-", 
    "BB+", "BB", "BB-", "B+", "B", "B-", "CCC", "CC", "C", "D"
  );

  public static void validateTenor(String tenor) {
    if (tenor == null || tenor.trim().isEmpty()) {
      throw new DataValidationException("tenor", tenor, "cannot be null or empty");
    }
    if (!VALID_TENORS.contains(tenor.toUpperCase())) {
      throw new DataValidationException("tenor", tenor, "must be one of: " + VALID_TENORS);
    }
  }

  public static void validateRegion(String region) {
    if (region == null || region.trim().isEmpty()) {
      throw new DataValidationException("region", region, "cannot be null or empty");
    }
    if (!VALID_REGIONS.contains(region.toUpperCase())) {
      throw new DataValidationException("region", region, "must be one of: " + VALID_REGIONS);
    }
  }

  public static void validateRating(String rating) {
    if (rating == null || rating.trim().isEmpty()) {
      throw new DataValidationException("rating", rating, "cannot be null or empty");
    }
    if (!VALID_RATINGS.contains(rating.toUpperCase())) {
      throw new DataValidationException("rating", rating, "must be one of: " + VALID_RATINGS);
    }
  }

  public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null) {
      throw new DataValidationException("Start date cannot be null");
    }
    if (endDate == null) {
      throw new DataValidationException("End date cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new DataValidationException("Start date cannot be after end date");
    }
    if (startDate.isBefore(LocalDate.now().minusYears(50))) {
      throw new DataValidationException("Start date cannot be more than 50 years in the past");
    }
    if (endDate.isAfter(LocalDate.now().plusDays(1))) {
      throw new DataValidationException("End date cannot be in the future");
    }
  }

  public static void validateDates(List<LocalDate> dates) {
    if (dates == null || dates.isEmpty()) {
      throw new DataValidationException("Dates list cannot be null or empty");
    }
    if (dates.size() > 100) {
      throw new DataValidationException("Cannot request more than 100 dates at once");
    }
    for (LocalDate date : dates) {
      if (date == null) {
        throw new DataValidationException("Date in list cannot be null");
      }
      if (date.isAfter(LocalDate.now())) {
        throw new DataValidationException("Date cannot be in the future: " + date);
      }
    }
  }

  public static void validateSector(String sector) {
    if (sector == null || sector.trim().isEmpty()) {
      throw new DataValidationException("sector", sector, "cannot be null or empty");
    }
    if (sector.length() > 50) {
      throw new DataValidationException("sector", sector, "cannot be longer than 50 characters");
    }
  }
}