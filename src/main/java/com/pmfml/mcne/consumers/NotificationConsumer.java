package com.pmfml.mcne.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.config.RabbitMQConfig;
import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.services.NotificationDispatcherService;

@Slf4j
@Component
public class NotificationConsumer {

  private final NotificationDispatcherService service;

  public NotificationConsumer(NotificationDispatcherService service) {
    this.service = service;
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
    service.processFromQueue(event);
  }
}
