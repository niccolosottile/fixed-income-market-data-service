package com.fixedincome.marketdata.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("Fixed Income Market Data API")
        .version("1.0.0")
        .description("API for fixed income asset valuation and market data")
        .contact(new Contact()
          .name("Niccolò Sottile")
          .email("niccolosottile@outlook.com"))
        .license(new License()
          .name("MIT License")
          .url("https://opensource.org/licenses/MIT")))
      .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
      .components(new Components()
        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
          .type(SecurityScheme.Type.APIKEY)
          .in(SecurityScheme.In.HEADER)
          .name("X-API-Key")));
  }
}
