package com.pmfml.mcne.services;

import com.pmfml.mcne.constants.MetadataKeys;
import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationEventType;
import com.pmfml.mcne.enums.NotificationStatus;
import com.pmfml.mcne.producers.NotificationProducer;
import com.pmfml.mcne.strategies.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for orchestrating the notification flow.
 * It handles the initial dispatch to the message broker and the
 * subsequent processing when messages are consumed from the queue.
 */
@Slf4j
@Service
public class NotificationDispatcherService {

  private final List<NotificationStrategy> strategies;
  private final NotificationLogService notificationLogService;
  private final NotificationProducer producer;
  private final WebSocketEventPublisher wsPublisher;
  private final DemoDelayHelper demoDelayHelper;

  public NotificationDispatcherService(List<NotificationStrategy> strategies,
      NotificationLogService notificationLogService,
      NotificationProducer producer,
      WebSocketEventPublisher wsPublisher,
      DemoDelayHelper demoDelayHelper) {
    this.strategies = strategies;
    this.notificationLogService = notificationLogService;
    this.producer = producer;
    this.wsPublisher = wsPublisher;
    this.demoDelayHelper = demoDelayHelper;
  }

  /**
   * Validates the requested channel, creates a pending log entry in the database,
   * and publishes the notification event to the message broker.
   *
   * @param request        the notification request payload
   * @param fromVisualizer whether the request originated from the demo Visualizer
   *                       client
   */
  public void dispatchToQueue(NotificationRequest request, boolean fromVisualizer) {
    NotificationRequest finalRequest = fromVisualizer ? markAsVisualizer(request) : request;

    boolean isSupported = strategies.stream().anyMatch(s -> s.supports(finalRequest.channel()));
    if (!isSupported) {
      throw new IllegalArgumentException("Unsupported notification channel: " + finalRequest.channel());
    }

    NotificationLog logEntry = notificationLogService.savePendingLog(finalRequest);

    // 1. Emit RECEIVED (API REST Box)
    wsPublisher.publish(new WebSocketNotificationEvent(
        logEntry.getId(), NotificationEventType.RECEIVED, finalRequest.channel().name(), finalRequest.message()));

    demoDelayHelper.applyDelay(finalRequest.metadata());

    producer.publish(new NotificationEvent(logEntry.getId(), finalRequest));

    wsPublisher.publish(new WebSocketNotificationEvent(
        logEntry.getId(), NotificationEventType.QUEUED, finalRequest.channel().name(), finalRequest.message()));
  }

  /**
   * Returns a copy of the request with the Visualizer metadata flag set,
   * so demo behaviour can be resolved downstream without leaking this concern
   * into the controller.
   */
  private NotificationRequest markAsVisualizer(NotificationRequest request) {
    Map<String, String> metadata = new HashMap<>();
    if (request.metadata() != null) {
      metadata.putAll(request.metadata());
    }
    metadata.put(MetadataKeys.IS_VISUALIZER_CLIENT, "true");
    return new NotificationRequest(
        request.recipient(), request.message(), request.channel(), metadata);
  }

  /**
   * Processes a notification event consumed from the queue. Resolves the
   * appropriate
   * strategy and attempts to send the message. Updates the log status
   * accordingly.
   * On failure, marks the log as FAILED and re-throws an
   * {@link AmqpRejectAndDontRequeueException}
   * so that RabbitMQ routes the message to the Dead Letter Queue.
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
      strategy.send(event.logId(), event.request());
      notificationLogService.updateStatus(event.logId(), NotificationStatus.SENT);
    } catch (Exception e) {
      // Attempt to mark the log as FAILED. If the log entry itself cannot be found,
      // log a warning but still ensure the message is sent to the DLQ.
      try {
        notificationLogService.updateStatus(event.logId(), NotificationStatus.FAILED);
      } catch (Exception updateEx) {
        log.warn("Could not update log status to FAILED for logId={}: {}", event.logId(), updateEx.getMessage());
      }
      wsPublisher.publish(new WebSocketNotificationEvent(
          event.logId(), NotificationEventType.DLQ, event.request().channel().name(),
          "Message routed to DLQ after retries"));
      throw new AmqpRejectAndDontRequeueException(
          "Strategy failed after retries for logId=" + event.logId() + ". Routing to DLQ.", e);
    }
  }
}
