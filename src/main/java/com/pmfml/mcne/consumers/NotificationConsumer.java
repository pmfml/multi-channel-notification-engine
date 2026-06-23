package com.pmfml.mcne.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.config.RabbitMQConfig;
import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.WebSocketEventPublisher;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;

@Slf4j
@Component
public class NotificationConsumer {

  private final NotificationDispatcherService service;
  private final WebSocketEventPublisher wsPublisher;

  public NotificationConsumer(NotificationDispatcherService service, WebSocketEventPublisher wsPublisher) {
    this.service = service;
    this.wsPublisher = wsPublisher;
  }

  /**
   * Listens to the notification queue and triggers the processing of incoming
   * requests.
   *
   * @param event the deserialized notification event payload containing the log
   *              ID
   */
  @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
  public void consume(NotificationEvent event) {
    log.info("Message received for log ID: {}", event.logId());
    
    wsPublisher.publish(new WebSocketNotificationEvent(
        event.logId(), 
        "PROCESSING", 
        event.request().channel().name(), 
        event.request().message()
    ));

    if (event.request().metadata() != null && event.request().metadata().containsKey("demoDelayMs")) {
      boolean isVisualizer = "true".equalsIgnoreCase(event.request().metadata().get("isVisualizerClient"));
      if (isVisualizer) {
        try {
          long delay = Long.parseLong(event.request().metadata().get("demoDelayMs"));
          if (delay > 0) {
            log.info("Applying simulated delay of {}ms for log ID: {}", delay, event.logId());
            Thread.sleep(delay);
          }
        } catch (InterruptedException | NumberFormatException e) {
          log.warn("Invalid demo delay or thread interrupted for log ID: {}", event.logId());
          Thread.currentThread().interrupt();
        }
      }
    }

    service.processFromQueue(event);
  }
}
