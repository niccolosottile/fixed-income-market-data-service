package com.fixedincome.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "alternative.api")
public class AlternativeApiProperties {
  
  private String baseUrl;
  private String apiKey;
  private int connectTimeout = 5000;
  private int readTimeout = 10000;
  private boolean enabled = false; // Disabled by default until API is configured
  
  // Fallback settings
  private boolean useFallbackData = true;
  private String fallbackDataSource = "static";
}
