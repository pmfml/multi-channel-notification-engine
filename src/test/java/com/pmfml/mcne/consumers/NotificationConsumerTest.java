package com.pmfml.mcne.consumers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.services.DemoDelayHelper;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.WebSocketEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

  @Mock
  private NotificationDispatcherService dispatcherService;

  @Mock
  private WebSocketEventPublisher wsPublisher;

  @Mock
  private DemoDelayHelper demoDelayHelper;

  private NotificationConsumer consumer;

  @BeforeEach
  void setUp() {
    // DemoDelayHelper is mocked, so delays are a no-op in these tests
    consumer = new NotificationConsumer(dispatcherService, wsPublisher, demoDelayHelper);
  }

  @Test
  @DisplayName("consume() should emit PROCESSING and delegate to dispatcher")
  void shouldEmitProcessingAndDelegate() {
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!", NotificationChannel.EMAIL, Map.of());
    NotificationEvent event = new NotificationEvent(UUID.randomUUID(), request);

    consumer.consume(event);

    verify(wsPublisher).publish(any(WebSocketNotificationEvent.class));
    verify(dispatcherService).processFromQueue(event);
  }

  @Test
  @DisplayName("consume() in demo mode should still delegate to dispatcher")
  void shouldDelegateInDemoMode() {
    consumer = new NotificationConsumer(dispatcherService, wsPublisher, demoDelayHelper);
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!", NotificationChannel.EMAIL,
        Map.of("isVisualizerClient", "true", "demoDelayMs", "0"));
    NotificationEvent event = new NotificationEvent(UUID.randomUUID(), request);

    consumer.consume(event);

    verify(dispatcherService).processFromQueue(event);
  }
}
