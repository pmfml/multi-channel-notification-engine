package com.pmfml.mcne.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.dtos.SystemStatusResponse;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.services.NotificationDispatcherService;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/status")
public class HealthCheckController {

  private final NotificationDispatcherService dispatcher;

  public HealthCheckController(NotificationDispatcherService dispatcher) {
    this.dispatcher = dispatcher;
  }

  @PostConstruct
  public void testDispatcher() {
    NotificationRequest request = new NotificationRequest(
        "teste@example.com",
        "Testando o Strategy Pattern diretamente no boot da aplicação!",
        NotificationChannel.EMAIL,
        Map.of("subject", "Teste de Integração")
    );
    dispatcher.dispatch(request);
  }

  @GetMapping
  public ResponseEntity<SystemStatusResponse> checkStatus() {
    SystemStatusResponse response = new SystemStatusResponse(
        "Up and Running",
        "Development",
        Instant.now());

    return ResponseEntity.ok(response);
  }

}
