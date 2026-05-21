package com.pmfml.mcne.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationStatus;
import com.pmfml.mcne.repositories.NotificationLogRepository;

@Service
public class NotificationLogService {

  private final NotificationLogRepository repository;

  public NotificationLogService(NotificationLogRepository repository) {
    this.repository = repository;
  }

  /**
   * Saves a new notification log with a PENDING status.
   *
   * @param request the notification request containing delivery details
   * @return the persisted NotificationLog entity
   */
  public NotificationLog savePendingLog(NotificationRequest request) {
    NotificationLog notificationLog = NotificationLog.builder()
        .recipient(request.recipient())
        .message(request.message())
        .channel(request.channel())
        .status(NotificationStatus.PENDING)
        .build();

    return repository.save(notificationLog);
  }

  public void updateStatus(UUID logId, NotificationStatus status) {
    repository.findById(logId).ifPresent(log -> {
      log.setStatus(status);
      repository.save(log);
    });
  }
}
