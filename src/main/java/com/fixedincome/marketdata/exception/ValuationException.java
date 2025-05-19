package com.fixedincome.marketdata.exception;

import org.springframework.http.HttpStatus;

class ValuationException extends ApiException {
  public ValuationException(String message) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }
  
  public ValuationException(String message, Throwable cause) {
    super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
