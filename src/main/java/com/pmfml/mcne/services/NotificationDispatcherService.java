package com.pmfml.mcne.services;

import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationStatus;
import com.pmfml.mcne.producers.NotificationProducer;
import com.pmfml.mcne.strategies.NotificationStrategy;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for orchestrating the notification flow.
 * It handles the initial dispatch to the message broker and the
 * subsequent processing when messages are consumed from the queue.
 */
@Service
public class NotificationDispatcherService {

  private final List<NotificationStrategy> strategies;
  private final NotificationLogService notificationLogService;
  private final NotificationProducer producer;

  public NotificationDispatcherService(List<NotificationStrategy> strategies,
      NotificationLogService notificationLogService,
      NotificationProducer producer) {
    this.strategies = strategies;
    this.notificationLogService = notificationLogService;
    this.producer = producer;
  }

  /**
   * Validates the requested channel, creates a pending log entry in the database,
   * and publishes the notification event to the message broker.
   *
   * @param request the notification request payload
   */
  public void dispatchToQueue(NotificationRequest request) {
    boolean isSupported = strategies.stream().anyMatch(s -> s.supports(request.channel()));
    if (!isSupported) {
      throw new IllegalArgumentException("Unsupported notification channel: " + request.channel());
    }

    NotificationLog log = notificationLogService.savePendingLog(request);
    producer.publish(new NotificationEvent(log.getId(), request));
  }

  /**
   * Processes a notification event consumed from the queue. Resolves the
   * appropriate
   * strategy and attempts to send the message. Updates the log status
   * accordingly.
   *
   * @param event the consumed notification event
   */
  public void processFromQueue(NotificationEvent event) {
    NotificationStrategy strategy = strategies.stream()
        .filter(s -> s.supports(event.request().channel()))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unsupported notification channel: " + event.request().channel()));

    try {
      strategy.send(event.request());
      notificationLogService.updateStatus(event.logId(), NotificationStatus.SENT);
    } catch (Exception e) {
      notificationLogService.updateStatus(event.logId(), NotificationStatus.FAILED);
      throw new AmqpRejectAndDontRequeueException("Exhausted retries. Message failed.", e);
    }
  }
}
