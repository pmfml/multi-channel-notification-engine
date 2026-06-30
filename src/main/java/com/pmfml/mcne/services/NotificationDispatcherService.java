package com.pmfml.mcne.services;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

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
  private final boolean demoMode;

  public NotificationDispatcherService(List<NotificationStrategy> strategies,
      NotificationLogService notificationLogService,
      NotificationProducer producer,
      WebSocketEventPublisher wsPublisher,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    this.strategies = strategies;
    this.notificationLogService = notificationLogService;
    this.producer = producer;
    this.wsPublisher = wsPublisher;
    this.demoMode = demoMode;
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

    NotificationLog logEntry = notificationLogService.savePendingLog(request);

    // 1. Emit RECEIVED (API REST Box)
    wsPublisher.publish(new WebSocketNotificationEvent(
        logEntry.getId(), NotificationEventType.RECEIVED, request.channel().name()
    ));

    if (demoMode) {
      applyDemoDelay(request);
    }

    producer.publish(new NotificationEvent(logEntry.getId(), request));

    wsPublisher.publish(new WebSocketNotificationEvent(
        logEntry.getId(), NotificationEventType.QUEUED, request.channel().name()
    ));
  }

  /**
   * Processes a notification event consumed from the queue. Resolves the appropriate
   * strategy and attempts to send the message. Updates the log status accordingly.
   * On failure, marks the log as FAILED and re-throws an {@link AmqpRejectAndDontRequeueException}
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
          event.logId(), NotificationEventType.DLQ, event.request().channel().name()
      ));
      throw new AmqpRejectAndDontRequeueException(
          "Strategy failed after retries for logId=" + event.logId() + ". Routing to DLQ.", e);
    }
  }

  /**
   * Applies an artificial delay for demo/visualizer purposes.
   * Only called when the {@code demo} profile is active.
   */
  private void applyDemoDelay(NotificationRequest request) {
    var metadata = request.metadata();
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
