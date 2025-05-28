package com.fixedincome.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@Configuration
@ConfigurationProperties(prefix = "fred.api")
@Validated
public class FredApiProperties {
  
  @NotBlank(message = "FRED API base URL must be configured")
  private String baseUrl = "https://api.stlouisfed.org/fred/series/observations";
  
  @NotBlank(message = "FRED API key must be configured")
  private String apiKey;
  
  @Positive(message = "Connect timeout must be positive")
  private int connectTimeout = 5000;
  
  @Positive(message = "Read timeout must be positive") 
  private int readTimeout = 10000;
  
  @PositiveOrZero(message = "Max retries cannot be negative")
  private int maxRetries = 3;
  
  @Positive(message = "Retry delay must be positive")
  private long retryDelayMs = 1000;
  
  private boolean enableCircuitBreaker = true;
  private int circuitBreakerFailureThreshold = 5;
  private long circuitBreakerTimeoutMs = 60000; // 1 minute
}
