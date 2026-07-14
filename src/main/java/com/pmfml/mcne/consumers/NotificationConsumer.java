package com.pmfml.mcne.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.config.RabbitMQConfig;
import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationEventType;
import com.pmfml.mcne.services.DemoDelayHelper;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.WebSocketEventPublisher;

@Slf4j
@Component
public class NotificationConsumer {

  private final NotificationDispatcherService service;
  private final WebSocketEventPublisher wsPublisher;
  private final DemoDelayHelper demoDelayHelper;

  public NotificationConsumer(NotificationDispatcherService service,
      WebSocketEventPublisher wsPublisher,
      DemoDelayHelper demoDelayHelper) {
    this.service = service;
    this.wsPublisher = wsPublisher;
    this.demoDelayHelper = demoDelayHelper;
  }

  /**
   * Listens to the notification queue and triggers the processing of incoming
   * requests.
   *
   * @param event the deserialized notification event payload containing the log
   *              ID
   */
  @RabbitListener(id = "notificationConsumer", queues = RabbitMQConfig.NOTIFICATION_QUEUE)
  public void consume(NotificationEvent event) {
    log.info("Message received for log ID: {}", event.logId());

    demoDelayHelper.applyDelay(event.request().metadata());

    wsPublisher.publish(new WebSocketNotificationEvent(
        event.logId(),
        NotificationEventType.PROCESSING,
        event.request().channel().name(),
        event.request().message()));

    demoDelayHelper.applyDelay(event.request().metadata());

    service.processFromQueue(event);
  }
}
