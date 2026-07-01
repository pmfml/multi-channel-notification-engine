package com.pmfml.mcne.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Centralised CORS configuration.
 *
 * <p>Allowed origins are driven by the {@code MCNE_ALLOWED_ORIGINS} environment
 * variable (comma-separated). Defaults to {@code http://localhost:5173} (Vite
 * dev server) so local development works out of the box.
 *
 * <p>This bean is picked up automatically by Spring Security's
 * {@code .cors()} DSL, so CORS preflight (OPTIONS) requests are handled
 * before the API key filter runs.
 */
@Configuration
public class CorsConfig {

  @Value("${mcne.cors.allowed-origins:http://localhost:5173}")
  private List<String> allowedOrigins;

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "X-API-Key", "X-MCNE-Client"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
