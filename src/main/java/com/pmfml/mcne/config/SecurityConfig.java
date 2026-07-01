package com.pmfml.mcne.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Security configuration for the MCNE API.
 *
 * <p>Authentication is performed via a static API key sent in the
 * {@code X-API-Key} request header. All API endpoints require the key.
 * WebSocket handshake ({@code /ws-mcne/**}) and Actuator endpoints are
 * intentionally left open.
 *
 * <p>Set the key via the {@code MCNE_API_KEY} environment variable.
 * A safe default ({@code dev-only-key}) is provided for local development only.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${mcne.security.api-key:dev-only-key}")
  private String apiKey;

  /**
   * Filter that validates the {@code X-API-Key} header on every request
   * that reaches protected endpoints.
   */
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> {}) // Delegate CORS to the CorsFilter bean from CorsConfig
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(apiKeyFilter(), UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            // Public: health check, actuator, WebSocket handshake
            .requestMatchers("/api/v1/status", "/actuator/**", "/ws-mcne/**").permitAll()
            // Everything else requires a valid API key (enforced by the filter)
            .anyRequest().authenticated()
        );
    return http.build();
  }

  OncePerRequestFilter apiKeyFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain chain) throws ServletException, IOException {
        String requestKey = request.getHeader("X-API-Key");
        if (apiKey.equals(requestKey)) {
          // Populate SecurityContext so that anyRequest().authenticated() is satisfied
          var auth = new UsernamePasswordAuthenticationToken(
              "api-client", null, List.of(new SimpleGrantedAuthority("ROLE_API")));
          SecurityContextHolder.getContext().setAuthentication(auth);
          chain.doFilter(request, response);
        } else {
          response.setStatus(HttpStatus.UNAUTHORIZED.value());
          response.setContentType("application/json");
          response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-API-Key header.\"}");
        }
      }

      @Override
      protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter for public paths (already permitted above, but guard here too)
        return path.startsWith("/api/v1/status")
            || path.startsWith("/actuator")
            || path.startsWith("/ws-mcne");
      }
    };
  }
}
