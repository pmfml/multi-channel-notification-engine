package com.pmfml.mcne.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ConfigController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class })
class ConfigControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RabbitListenerEndpointRegistry registry;

  @Test
  @DisplayName("PUT /api/v1/config/concurrency with count=0 should stop container")
  void shouldStopContainerWhenCountIsZero() throws Exception {
    SimpleMessageListenerContainer container = mock(SimpleMessageListenerContainer.class);
    when(registry.getListenerContainer("notificationConsumer")).thenReturn(container);

    mockMvc.perform(put("/api/v1/config/concurrency").param("count", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Concurrency updated to 0"));

    verify(container).stop();
    verify(container, never()).start();
  }

  @Test
  @DisplayName("PUT /api/v1/config/concurrency with count>0 should start and scale container")
  void shouldStartAndScaleContainer() throws Exception {
    SimpleMessageListenerContainer container = mock(SimpleMessageListenerContainer.class);
    when(registry.getListenerContainer("notificationConsumer")).thenReturn(container);
    when(container.isRunning()).thenReturn(false);

    mockMvc.perform(put("/api/v1/config/concurrency").param("count", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Concurrency updated to 5"));

    verify(container).start();
    verify(container).setConcurrentConsumers(5);
    verify(container).setMaxConcurrentConsumers(5);
  }
  
  @Test
  @DisplayName("PUT /api/v1/config/concurrency should return 500 when container not found")
  void shouldReturn500WhenContainerNotFound() throws Exception {
    when(registry.getListenerContainer("notificationConsumer")).thenReturn(null);

    mockMvc.perform(put("/api/v1/config/concurrency").param("count", "5"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("Listener container 'notificationConsumer' not found."));
  }
}
