package com.pmfml.mcne.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HealthCheckController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class })
class HealthCheckControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("GET /api/v1/status should return 200 with status, environment, and timestamp")
  void shouldReturnSystemStatus() throws Exception {
    mockMvc.perform(get("/api/v1/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Up and Running"))
        .andExpect(jsonPath("$.environment").value("default"))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
