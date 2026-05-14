package com.pmfml.mcne.services;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.strategies.NotificationStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationDispatcherService {

  private final List<NotificationStrategy> strategies;

  public NotificationDispatcherService(List<NotificationStrategy> strategies) {
    this.strategies = strategies;
  }

  public void dispatch(NotificationRequest request) {
    NotificationStrategy strategy = strategies.stream()
        .filter(s -> s.supports(request.channel()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Canal de notificação não suportado: " + request.channel()));

    strategy.send(request);
  }
}
