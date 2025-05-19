package com.fixedincome.marketdata.exception;

import org.springframework.http.HttpStatus;

class ResourceNotFoundException extends ApiException {
  public ResourceNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }
}
