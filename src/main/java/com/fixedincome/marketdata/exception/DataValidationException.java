package com.fixedincome.marketdata.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when data validation fails
 */
public class DataValidationException extends ApiException {
  
  public DataValidationException(String message) {
    super("Data validation failed: " + message, HttpStatus.BAD_REQUEST);
  }
  
  public DataValidationException(String field, String value, String reason) {
    super(String.format("Invalid %s value '%s': %s", field, value, reason), HttpStatus.BAD_REQUEST);
  }
}