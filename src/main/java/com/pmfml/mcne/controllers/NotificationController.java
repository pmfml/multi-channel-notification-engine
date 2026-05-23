package com.pmfml.mcne.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.NotificationDlqService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationDispatcherService dispatcherService;
  private final NotificationDlqService dlqService;

  public NotificationController(NotificationDispatcherService dispatcherService, NotificationDlqService dlqService) {
    this.dispatcherService = dispatcherService;
    this.dlqService = dlqService;
  }

  @PostMapping
  public ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest request) {
    dispatcherService.dispatchToQueue(request);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/dlq/reprocess")
  public ResponseEntity<Map<String, Object>> reprocessDlq() {
    int count = dlqService.reprocessMessages();
    return ResponseEntity.ok(Map.of("message", count + " messages reprocessed successfully."));
  }
}