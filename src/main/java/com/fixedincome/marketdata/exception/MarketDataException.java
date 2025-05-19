package com.fixedincome.marketdata.exception;

import org.springframework.http.HttpStatus;

class MarketDataException extends ApiException {
  public MarketDataException(String message) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }
  
  public MarketDataException(String message, Throwable cause) {
    super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
