package com.fixedincome.marketdata.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

  @Value("${api.key:default-api-key-for-dev}")
  private String apiKey;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    
    // Skip authentication for permitted endpoints
    if (request.getRequestURI().contains("/api/v1/health") ||
      request.getRequestURI().contains("/swagger-ui") ||
      request.getRequestURI().contains("/v3/api-docs") ||
      request.getRequestURI().contains("/h2-console")) {
      filterChain.doFilter(request, response);
      return;
    }
    
    // Get API key from header
    String requestApiKey = request.getHeader("X-API-Key");
    
    // Validate API key
    if (requestApiKey != null && requestApiKey.equals(apiKey)) {
      // Create authentication token with ROLE_API authority
      UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        "API_CLIENT", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
      
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } else {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("Invalid or missing API key");
      return;
    }
    
    filterChain.doFilter(request, response);
  }
}
