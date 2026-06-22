package com.pmfml.mcne.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationStatus;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

  /**
   * Retrieves all notification logs with the given status.
   * Reserved for future use in a dedicated query endpoint (e.g., GET /api/v1/notifications?status=FAILED).
   */
  List<NotificationLog> findByStatus(NotificationStatus status);
}
