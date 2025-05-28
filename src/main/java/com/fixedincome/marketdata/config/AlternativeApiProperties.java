package com.fixedincome.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@Configuration
@ConfigurationProperties(prefix = "alternative.api")
@Validated
public class AlternativeApiProperties {
  
  private String baseUrl;
  
  private String apiKey;
  
  @Positive(message = "Connect timeout must be positive")
  private int connectTimeout = 5000;
  
  @Positive(message = "Read timeout must be positive")
  private int readTimeout = 10000;
  
  private boolean enabled = false; // Disabled by default until API is configured
  
  // Retry and resilience settings
  @PositiveOrZero(message = "Max retries cannot be negative")
  private int maxRetries = 2;
  
  @Positive(message = "Retry delay must be positive")
  private long retryDelayMs = 1500;
  
  // Fallback settings
  private boolean useFallbackData = true;
  private String fallbackDataSource = "static";
  
  private boolean enableCircuitBreaker = true;
  private int circuitBreakerFailureThreshold = 3;
  private long circuitBreakerTimeoutMs = 30000; // 30 seconds
}
