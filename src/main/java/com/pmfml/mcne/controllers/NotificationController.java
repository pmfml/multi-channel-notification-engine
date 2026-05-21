package com.pmfml.mcne.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.services.NotificationDispatcherService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationDispatcherService dispatcherService;

  public NotificationController(NotificationDispatcherService dispatcherService) {
    this.dispatcherService = dispatcherService;
  }

  @PostMapping
  public ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest request) {
    dispatcherService.dispatchToQueue(request);
    return ResponseEntity.accepted().build();
  }
}