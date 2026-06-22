package com.pmfml.mcne.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pmfml.mcne.dtos.SystemStatusResponse;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * REST Controller exposing a lightweight system status endpoint.
 * Returns the current operational state and environment of the application.
 */
@RestController
@RequestMapping("/api/v1/status")
public class HealthCheckController {

  /**
   * Returns the current health status and environment of the application.
   *
   * @return a {@link SystemStatusResponse} containing status, environment, and timestamp
   */
  @GetMapping
  public ResponseEntity<SystemStatusResponse> checkStatus() {
    SystemStatusResponse response = new SystemStatusResponse(
        "Up and Running",
        "Development",
        Instant.now());

    return ResponseEntity.ok(response);
  }

}
