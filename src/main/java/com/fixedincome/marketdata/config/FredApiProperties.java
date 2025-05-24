package com.fixedincome.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "fred.api")
public class FredApiProperties {
  
  private String baseUrl;
  private String apiKey;
  private int connectTimeout;
  private int readTimeout;
}
