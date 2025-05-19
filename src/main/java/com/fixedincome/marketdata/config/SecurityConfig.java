package com.fixedincome.marketdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())  // Disable CSRF for API service
      .authorizeHttpRequests(auth -> auth
        // Public endpoints
        .requestMatchers("/api/v1/health").permitAll()
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .requestMatchers("/h2-console/**").permitAll()  // Only for dev environment
        // Secure all other endpoints
        .requestMatchers("/api/**").authenticated()
        .anyRequest().authenticated()
      )
      // For H2 console in dev environment
      .headers(headers -> 
        headers.frameOptions(frameOptions -> frameOptions.disable())
      )
      // Stateless session management
      .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      // API Key filter
      .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        
    return http.build();
  }
  
  @Bean
  public ApiKeyAuthFilter apiKeyAuthFilter() {
    return new ApiKeyAuthFilter();
  }
}
