package com.pmfml.mcne.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.config.RabbitMQConfig;
import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationEventType;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.WebSocketEventPublisher;

@Slf4j
@Component
public class NotificationConsumer {

  private final NotificationDispatcherService service;
  private final WebSocketEventPublisher wsPublisher;
  private final boolean demoMode;

  public NotificationConsumer(NotificationDispatcherService service,
      WebSocketEventPublisher wsPublisher,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    this.service = service;
    this.wsPublisher = wsPublisher;
    this.demoMode = demoMode;
  }

  /**
   * Listens to the notification queue and triggers the processing of incoming
   * requests.
   *
   * @param event the deserialized notification event payload containing the log ID
   */
  @RabbitListener(id = "notificationConsumer", queues = RabbitMQConfig.NOTIFICATION_QUEUE)
  public void consume(NotificationEvent event) {
    log.info("Message received for log ID: {}", event.logId());

    applyDemoDelay(event);

    wsPublisher.publish(new WebSocketNotificationEvent(
        event.logId(),
        NotificationEventType.PROCESSING,
        event.request().channel().name(),
        event.request().message()
    ));

    applyDemoDelay(event);

    service.processFromQueue(event);
  }

  /**
   * Applies an artificial delay for demo/visualizer purposes.
   * No-op when the {@code demo} profile is inactive.
   */
  private void applyDemoDelay(NotificationEvent event) {
    if (!demoMode) return;
    var metadata = event.request().metadata();
    if (metadata == null || !metadata.containsKey("demoDelayMs")) return;
    if (!"true".equalsIgnoreCase(metadata.get("isVisualizerClient"))) return;
    try {
      long delay = Long.parseLong(metadata.get("demoDelayMs"));
      if (delay > 0) Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (NumberFormatException ignored) {
      // malformed value — skip delay silently
    }
  }
}
