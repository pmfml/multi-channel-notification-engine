package com.pmfml.mcne.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.pmfml.mcne.config.SecurityConfig;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.NotificationDlqService;

/**
 * Security-focused tests for the Notification API.
 * Loads the real {@link SecurityConfig} and verifies that the API key
 * filter correctly enforces authentication on protected endpoints.
 */
@WebMvcTest(controllers = NotificationController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "mcne.security.api-key=test-key-123")
class NotificationControllerSecurityTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockBean
  private NotificationDispatcherService dispatcherService;

  @MockBean
  private NotificationDlqService dlqService;

  @BeforeEach
  void setUp() {
    // Build MockMvc with the full Spring Security filter chain applied
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  private static final String VALID_BODY = """
      {"recipient":"u@e.com","message":"Hi","channel":"EMAIL"}
      """;

  @Test
  @DisplayName("Should return 401 Unauthorized when X-API-Key header is missing")
  void shouldReturn401WhenApiKeyIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when X-API-Key is wrong")
  void shouldReturn401WhenApiKeyIsWrong() throws Exception {
    mockMvc.perform(post("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-API-Key", "wrong-key")
        .content(VALID_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should return 202 Accepted when X-API-Key is correct")
  void shouldReturn202WhenApiKeyIsCorrect() throws Exception {
    doNothing().when(dispatcherService).dispatchToQueue(any(NotificationRequest.class), anyBoolean());

    mockMvc.perform(post("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-API-Key", "test-key-123")
        .content(VALID_BODY))
        .andExpect(status().isAccepted());
  }
}
