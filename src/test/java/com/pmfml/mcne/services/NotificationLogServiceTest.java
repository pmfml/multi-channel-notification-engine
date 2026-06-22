package com.pmfml.mcne.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationStatus;
import com.pmfml.mcne.exceptions.ResourceNotFoundException;
import com.pmfml.mcne.repositories.NotificationLogRepository;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {

  @Mock
  private NotificationLogRepository repository;

  @InjectMocks
  private NotificationLogService service;

  @Test
  @DisplayName("Should save a new notification log with PENDING status")
  void shouldSavePendingLog() {
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!", NotificationChannel.EMAIL, Map.of());

    NotificationLog savedLog = NotificationLog.builder()
        .id(UUID.randomUUID())
        .recipient("user@example.com")
        .message("Hello!")
        .channel(NotificationChannel.EMAIL)
        .status(NotificationStatus.PENDING)
        .build();

    when(repository.save(any())).thenReturn(savedLog);

    NotificationLog result = service.savePendingLog(request);

    assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
    assertThat(result.getRecipient()).isEqualTo("user@example.com");
    assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);

    ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
  }

  @Test
  @DisplayName("Should update notification log status to SENT when log exists")
  void shouldUpdateStatusToSentWhenLogExists() {
    UUID logId = UUID.randomUUID();
    NotificationLog log = NotificationLog.builder()
        .id(logId)
        .status(NotificationStatus.PENDING)
        .build();

    when(repository.findById(logId)).thenReturn(Optional.of(log));
    when(repository.save(any())).thenReturn(log);

    service.updateStatus(logId, NotificationStatus.SENT);

    assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
    verify(repository).save(log);
  }

  @Test
  @DisplayName("Should update notification log status to FAILED when log exists")
  void shouldUpdateStatusToFailedWhenLogExists() {
    UUID logId = UUID.randomUUID();
    NotificationLog log = NotificationLog.builder()
        .id(logId)
        .status(NotificationStatus.PENDING)
        .build();

    when(repository.findById(logId)).thenReturn(Optional.of(log));
    when(repository.save(any())).thenReturn(log);

    service.updateStatus(logId, NotificationStatus.FAILED);

    assertThat(log.getStatus()).isEqualTo(NotificationStatus.FAILED);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when log ID does not exist")
  void shouldThrowResourceNotFoundExceptionWhenLogDoesNotExist() {
    UUID logId = UUID.randomUUID();
    when(repository.findById(logId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateStatus(logId, NotificationStatus.SENT))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(logId.toString());

    verify(repository, never()).save(any());
  }
}
