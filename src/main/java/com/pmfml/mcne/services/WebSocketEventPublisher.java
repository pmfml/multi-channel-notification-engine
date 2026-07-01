package com.pmfml.mcne.services;

import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketEventPublisher {

  private final SimpMessagingTemplate messagingTemplate;
  private final boolean demoMode;

  public WebSocketEventPublisher(SimpMessagingTemplate messagingTemplate,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    this.messagingTemplate = messagingTemplate;
    this.demoMode = demoMode;
  }

  /**
   * Publishes a notification event to the WebSocket topic "/topic/notifications".
   * This is wrapped to avoid coupling all services directly to SimpMessagingTemplate.
   *
   * <p>Privacy: in production the message body is stripped before broadcasting,
   * since the topic is shared by all connected clients. In demo mode the message
   * is kept so the Visualizer terminal can display it.
   *
   * @param event the event to broadcast
   */
  public void publish(WebSocketNotificationEvent event) {
    WebSocketNotificationEvent outbound = demoMode
        ? event
        : new WebSocketNotificationEvent(event.logId(), event.eventType(), event.channel(), null);

    log.debug("Publishing WS Event: {} for Log ID: {}", outbound.eventType(), outbound.logId());
    try {
      messagingTemplate.convertAndSend("/topic/notifications", outbound);
    } catch (Exception e) {
      log.warn("Failed to publish WebSocket event for Log ID {}: {}", outbound.logId(), e.getMessage());
    }
  }
}
