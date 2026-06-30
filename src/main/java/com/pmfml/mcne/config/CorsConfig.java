package com.pmfml.mcne.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Centralised CORS configuration.
 *
 * <p>Allowed origins are driven by the {@code MCNE_ALLOWED_ORIGINS} environment
 * variable (comma-separated). Defaults to {@code http://localhost:5173} (Vite
 * dev server) so local development works out of the box without opening the API
 * to every origin.
 *
 * <p>Production deployments must set {@code MCNE_ALLOWED_ORIGINS} explicitly.
 */
@Configuration
public class CorsConfig {

  @Value("${mcne.cors.allowed-origins:http://localhost:5173}")
  private List<String> allowedOrigins;

  @Bean
  CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "X-API-Key", "X-MCNE-Client"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
