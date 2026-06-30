package com.pmfml.mcne.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Demo-only configuration, active exclusively when the {@code demo} Spring
 * profile is enabled (e.g. {@code --spring.profiles.active=demo}).
 *
 * <p>Registers a servlet filter that marks requests coming from the Visualizer
 * frontend by inspecting the {@code X-MCNE-Client: Visualizer} header. This
 * flag is then read by strategies to enable artificial delays and simulated
 * errors — behaviour that must never reach production.
 *
 * <p>When this profile is inactive the header is silently ignored everywhere.
 */
@Configuration
@Profile("demo")
public class DemoConfig {

  private static final Logger log = LoggerFactory.getLogger(DemoConfig.class);

  /** Header sent by the Visualizer frontend. */
  public static final String VISUALIZER_HEADER = "X-MCNE-Client";
  /** Expected header value that enables demo behaviour. */
  public static final String VISUALIZER_VALUE = "Visualizer";

  @Bean
  @Order(1)
  OncePerRequestFilter visualizerMarkerFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain chain)
          throws ServletException, java.io.IOException {
        if (VISUALIZER_VALUE.equalsIgnoreCase(request.getHeader(VISUALIZER_HEADER))) {
          log.debug("Demo mode: Visualizer client detected");
          request.setAttribute("isVisualizerClient", true);
        }
        chain.doFilter(request, response);
      }
    };
  }
}
