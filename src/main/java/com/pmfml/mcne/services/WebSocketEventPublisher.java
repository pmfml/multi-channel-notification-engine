package com.pmfml.mcne.services;

import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public WebSocketEventPublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Publishes a notification event to the WebSocket topic "/topic/notifications".
   * This is wrapped to avoid coupling all services directly to SimpMessagingTemplate.
   *
   * @param event The event to broadcast.
   */
  public void publish(WebSocketNotificationEvent event) {
    log.debug("Publishing WS Event: {} for Log ID: {}", event.eventType(), event.logId());
    try {
      messagingTemplate.convertAndSend("/topic/notifications", event);
    } catch (Exception e) {
      log.warn("Failed to publish WebSocket event for Log ID {}: {}", event.logId(), e.getMessage());
    }
  }
}
