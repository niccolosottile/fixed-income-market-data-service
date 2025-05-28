package com.fixedincome.marketdata.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when external data providers are unavailable or failing
 */
public class ProviderUnavailableException extends ApiException {
  
  public ProviderUnavailableException(String providerName) {
    super("Provider " + providerName + " is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE);
  }
  
  public ProviderUnavailableException(String providerName, Throwable cause) {
    super("Provider " + providerName + " is currently unavailable", cause, HttpStatus.SERVICE_UNAVAILABLE);
  }
}