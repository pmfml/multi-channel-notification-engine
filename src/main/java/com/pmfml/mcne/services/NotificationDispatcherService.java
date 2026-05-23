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

  public void dispatchToQueue(NotificationRequest request) {
    boolean isSupported = strategies.stream().anyMatch(s -> s.supports(request.channel()));
    if (!isSupported) {
      throw new IllegalArgumentException("Unsupported notification channel: " + request.channel());
    }

    NotificationLog log = notificationLogService.savePendingLog(request);
    producer.publish(new NotificationEvent(log.getId(), request));
  }

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
