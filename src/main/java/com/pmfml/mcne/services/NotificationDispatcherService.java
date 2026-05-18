package com.pmfml.mcne.services;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.strategies.NotificationStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationDispatcherService {

  private final List<NotificationStrategy> strategies;
  private final NotificationLogService notificationLogService;

  public NotificationDispatcherService(List<NotificationStrategy> strategies,
      NotificationLogService notificationLogService) {
    this.strategies = strategies;
    this.notificationLogService = notificationLogService;
  }

  public void dispatch(NotificationRequest request) {
    NotificationStrategy strategy = strategies.stream()
        .filter(s -> s.supports(request.channel()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported notification channel: " + request.channel()));

    notificationLogService.savePendingLog(request);

    strategy.send(request);
  }
}
