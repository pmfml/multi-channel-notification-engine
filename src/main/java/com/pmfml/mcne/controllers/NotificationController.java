package com.pmfml.mcne.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.services.NotificationDispatcherService;
import com.pmfml.mcne.services.NotificationDlqService;

import jakarta.validation.Valid;

/**
 * REST Controller for exposing notification-related API endpoints.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationDispatcherService dispatcherService;
  private final NotificationDlqService dlqService;

  public NotificationController(NotificationDispatcherService dispatcherService,
      NotificationDlqService dlqService) {
    this.dispatcherService = dispatcherService;
    this.dlqService = dlqService;
  }

  /**
   * Accepts a new notification request and dispatches it for asynchronous processing.
   *
   * <p>When the {@code demo} profile is active, an optional {@code X-MCNE-Client: Visualizer}
   * header marks the request so the dispatcher can apply artificial delays and simulated errors.
   * The header is silently ignored in production.
   *
   * @param request      the notification payload
   * @param clientHeader optional client identifier header (used in demo mode only)
   * @return HTTP 202 Accepted
   */
  @PostMapping
  public ResponseEntity<Void> sendNotification(
      @Valid @RequestBody NotificationRequest request,
      @RequestHeader(value = "X-MCNE-Client", required = false) String clientHeader) {

    NotificationRequest finalRequest = request;
    if ("Visualizer".equalsIgnoreCase(clientHeader)) {
      Map<String, String> updatedMetadata = new HashMap<>();
      if (request.metadata() != null) {
        updatedMetadata.putAll(request.metadata());
      }
      updatedMetadata.put("isVisualizerClient", "true");
      finalRequest = new NotificationRequest(
          request.recipient(), request.message(), request.channel(), updatedMetadata);
    }

    dispatcherService.dispatchToQueue(finalRequest);
    return ResponseEntity.accepted().build();
  }

  /**
   * Triggers the reprocessing of all messages currently residing in the Dead Letter Queue.
   *
   * @return a map containing the result message and the count of reprocessed messages
   */
  @PostMapping("/dlq/reprocess")
  public ResponseEntity<Map<String, Object>> reprocessDlq() {
    int count = dlqService.reprocessMessages();
    return ResponseEntity.ok(Map.of("message", count + " messages reprocessed successfully."));
  }
}
