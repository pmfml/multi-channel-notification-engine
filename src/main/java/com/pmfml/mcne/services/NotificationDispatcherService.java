package com.pmfml.mcne.services;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.producers.NotificationProducer;
import com.pmfml.mcne.strategies.NotificationStrategy;
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

    notificationLogService.savePendingLog(request);
    producer.publish(request);
  }

  public void processFromQueue(NotificationRequest request) {
    NotificationStrategy strategy = strategies.stream()
        .filter(s -> s.supports(request.channel()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported notification channel: " + request.channel()));

    strategy.send(request);
  }
}
