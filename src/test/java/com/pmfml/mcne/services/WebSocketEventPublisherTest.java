package com.pmfml.mcne.services;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationEventType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebSocketEventPublisherTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Test
  @DisplayName("In demo mode, message body should be transmitted")
  void shouldTransmitMessageInDemoMode() {
    WebSocketEventPublisher publisher = new WebSocketEventPublisher(messagingTemplate, true);
    var event = new WebSocketNotificationEvent(UUID.randomUUID(), NotificationEventType.SENT, "EMAIL", "Hello!");

    publisher.publish(event);

    ArgumentCaptor<WebSocketNotificationEvent> captor = ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), captor.capture());
    assertThat(captor.getValue().message()).isEqualTo("Hello!");
  }

  @Test
  @DisplayName("In production mode, message body should be stripped (null)")
  void shouldStripMessageInProductionMode() {
    WebSocketEventPublisher publisher = new WebSocketEventPublisher(messagingTemplate, false);
    var event = new WebSocketNotificationEvent(UUID.randomUUID(), NotificationEventType.SENT, "EMAIL", "Secret content");

    publisher.publish(event);

    ArgumentCaptor<WebSocketNotificationEvent> captor = ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), captor.capture());
    assertThat(captor.getValue().message()).isNull();
    assertThat(captor.getValue().logId()).isEqualTo(event.logId());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.SENT);
  }

  @Test
  @DisplayName("Should not throw when SimpMessagingTemplate fails")
  void shouldHandleTemplateException() {
    WebSocketEventPublisher publisher = new WebSocketEventPublisher(messagingTemplate, true);
    doThrow(new RuntimeException("Connection lost")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

    var event = new WebSocketNotificationEvent(UUID.randomUUID(), NotificationEventType.QUEUED, "SMS", "Test");

    // Should not throw
    publisher.publish(event);
  }
}
