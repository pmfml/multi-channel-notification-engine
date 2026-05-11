package com.pmfml.mcne.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pmfml.mcne.dtos.SystemStatusResponse;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/status")
public class HealthCheckController {

  @GetMapping
  public ResponseEntity<SystemStatusResponse> checkStatus() {
    SystemStatusResponse response = new SystemStatusResponse(
        "Up and Running",
        "Development",
        Instant.now());

    return ResponseEntity.ok(response);
  }

}
