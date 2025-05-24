package com.fixedincome.marketdata.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
  private final FredApiProperties fredApiProperties;

  @Autowired
  public RestTemplateConfig(FredApiProperties fredApiProperties) {
    this.fredApiProperties = fredApiProperties;
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplateBuilder()
      .setConnectTimeout(Duration.ofMillis(fredApiProperties.getConnectTimeout()))
      .setReadTimeout(Duration.ofMillis(fredApiProperties.getReadTimeout()))
      .build();
  }
}