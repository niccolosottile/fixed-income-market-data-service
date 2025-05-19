package com.fixedincome.marketdata.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

  // Handle specific custom exception
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> handleApiException(
    ApiException exception, 
    WebRequest request) {
    
    ErrorDetails errorDetails = new ErrorDetails(
      LocalDateTime.now(),
      exception.getMessage(),
      request.getDescription(false),
      exception.getStatus().value());
    
    return new ResponseEntity<>(errorDetails, exception.getStatus());
  }
  
  // Handle validation exceptions
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidationExceptions(
    MethodArgumentNotValidException ex) {
    
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    
    ValidationErrorDetails errorDetails = new ValidationErrorDetails(
      LocalDateTime.now(),
      "Validation failed",
      errors,
      HttpStatus.BAD_REQUEST.value());
    
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }
  
  // Handle general exceptions
  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleGlobalException(
    Exception exception,
    WebRequest request) {
    
    ErrorDetails errorDetails = new ErrorDetails(
      LocalDateTime.now(),
      exception.getMessage(),
      request.getDescription(false),
      HttpStatus.INTERNAL_SERVER_ERROR.value());

    return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
  }
  
  // Error response models
  static class ErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private int status;
    
    public ErrorDetails(LocalDateTime timestamp, String message, String details, int status) {
      this.timestamp = timestamp;
      this.message = message;
      this.details = details;
      this.status = status;
    }
    
    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public int getStatus() { return status; }
  }
  
  static class ValidationErrorDetails extends ErrorDetails {
    private Map<String, String> validationErrors;
    
    public ValidationErrorDetails(LocalDateTime timestamp, String message, 
      Map<String, String> validationErrors, int status) {
      super(timestamp, message, "Validation failed", status);
      this.validationErrors = validationErrors;
    }
    
    // Getter
    public Map<String, String> getValidationErrors() { return validationErrors; }
  }
}
