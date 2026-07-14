package com.pmfml.mcne.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.NotificationDlqService;

// Security auto-configuration is excluded here because @WebMvcTest is a controller
// slice test focused on MVC behaviour (validation, routing, serialization).
// API key authentication is covered in NotificationControllerSecurityTest.
@WebMvcTest(controllers = NotificationController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class })
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private NotificationDispatcherService dispatcherService;

  @MockBean
  private NotificationDlqService dlqService;

  // Helper to perform a POST — no security in this slice test
  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postWithKey(String url) {
    return post(url);
  }

  @Test
  @DisplayName("Should return 202 Accepted when a valid EMAIL notification request is received")
  void shouldReturn202WhenValidEmailRequest() throws Exception {
    String requestBody = """
        {
          "recipient": "user@example.com",
          "message": "Welcome to our platform!",
          "channel": "EMAIL",
          "metadata": {}
        }
        """;

    mockMvc.perform(postWithKey("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isAccepted());

    verify(dispatcherService).dispatchToQueue(any(NotificationRequest.class), anyBoolean());
  }

  @Test
  @DisplayName("Should return 202 Accepted when a valid SMS notification request is received")
  void shouldReturn202WhenValidSmsRequest() throws Exception {
    String requestBody = """
        {
          "recipient": "+5511999999999",
          "message": "Your verification code is 1234.",
          "channel": "SMS",
          "metadata": {}
        }
        """;

    mockMvc.perform(postWithKey("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isAccepted());
  }

  @Test
  @DisplayName("Should return 400 Bad Request when recipient is blank")
  void shouldReturn400WhenRecipientIsBlank() throws Exception {
    String requestBody = """
        {
          "recipient": "",
          "message": "Hello!",
          "channel": "EMAIL",
          "metadata": {}
        }
        """;

    mockMvc.perform(postWithKey("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Failed"))
        .andExpect(jsonPath("$.fields.recipient").exists());
  }

  @Test
  @DisplayName("Should return 400 Bad Request when message is null")
  void shouldReturn400WhenMessageIsNull() throws Exception {
    String requestBody = """
        {
          "recipient": "user@example.com",
          "channel": "EMAIL",
          "metadata": {}
        }
        """;

    mockMvc.perform(postWithKey("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.message").exists());
  }

  @Test
  @DisplayName("Should return 400 Bad Request when channel is null")
  void shouldReturn400WhenChannelIsNull() throws Exception {
    String requestBody = """
        {
          "recipient": "user@example.com",
          "message": "Hello!",
          "metadata": {}
        }
        """;

    mockMvc.perform(postWithKey("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.channel").exists());
  }

  @Test
  @DisplayName("Should return 400 Bad Request when dispatcher throws IllegalArgumentException")
  void shouldReturn400WhenDispatcherThrowsIllegalArgument() throws Exception {
    doThrow(new IllegalArgumentException("Unsupported notification channel: PUSH"))
        .when(dispatcherService).dispatchToQueue(any(), anyBoolean());

    String requestBody = """
        {
          "recipient": "user@example.com",
          "message": "Hello!",
          "channel": "PUSH",
          "metadata": {}
        }
        """;

    mockMvc.perform(postWithKey("/api/v1/notifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Unsupported notification channel: PUSH"));
  }

  @Test
  @DisplayName("Should return 200 OK with reprocessed count after DLQ reprocessing")
  void shouldReturn200AfterDlqReprocess() throws Exception {
    when(dlqService.reprocessMessages()).thenReturn(3);

    mockMvc.perform(postWithKey("/api/v1/notifications/dlq/reprocess"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("3 messages reprocessed successfully."));
  }

  @Test
  @DisplayName("Should return 200 OK with zero count when DLQ is empty")
  void shouldReturn200WithZeroWhenDlqIsEmpty() throws Exception {
    when(dlqService.reprocessMessages()).thenReturn(0);

    mockMvc.perform(postWithKey("/api/v1/notifications/dlq/reprocess"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("0 messages reprocessed successfully."));
  }
}
