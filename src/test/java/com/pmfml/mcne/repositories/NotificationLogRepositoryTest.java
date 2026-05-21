package com.pmfml.mcne.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationLogRepositoryTest {

  @Autowired
  private NotificationLogRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  @Test
  @DisplayName("Should successfully save a notification log and generate UUID and timestamp")
  void shouldSaveNotificationLogSuccessfully() {
    NotificationLog log = NotificationLog.builder()
        .recipient("test@example.com")
        .message("Hello Test!")
        .channel(NotificationChannel.EMAIL)
        .status(NotificationStatus.PENDING)
        .build();

    NotificationLog savedLog = repository.saveAndFlush(log);

    assertThat(savedLog.getId()).isNotNull();
    assertThat(savedLog.getCreatedAt()).isNotNull();
    assertThat(savedLog.getRecipient()).isEqualTo("test@example.com");
  }

  @Test
  @DisplayName("Should find notification logs by a specific status")
  void shouldFindNotificationLogsByStatus() {
    NotificationLog log1 = NotificationLog.builder()
        .recipient("a@b.com")
        .message("Hello Test!")
        .channel(NotificationChannel.EMAIL)
        .status(NotificationStatus.PENDING)
        .build();

    NotificationLog log2 = NotificationLog.builder()
        .recipient("b@c.com")
        .message("Hello Test!")
        .channel(NotificationChannel.SMS)
        .status(NotificationStatus.PENDING)
        .build();

    NotificationLog log3 = NotificationLog.builder()
        .recipient("abc")
        .message("Hello Test!")
        .channel(NotificationChannel.SMS)
        .status(NotificationStatus.SENT)
        .build();

    repository.saveAll(List.of(log1, log2, log3));

    List<NotificationLog> notificationLogs = repository.findByStatus(NotificationStatus.PENDING);

    assertThat(notificationLogs).hasSize(2);
  }
}