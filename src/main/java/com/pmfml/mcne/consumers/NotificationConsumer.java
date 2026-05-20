package com.pmfml.mcne.consumers;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.config.RabbitMQConfig;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.services.NotificationDispatcherService;

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
   * @param request the deserialized notification request payload
   */
  @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
  public void consume(NotificationRequest request) {
    System.out.println("Message received: " + request.message());
    service.processFromQueue(request);
  }
}
