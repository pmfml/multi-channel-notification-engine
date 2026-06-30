package com.pmfml.mcne.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationEventType;
import com.pmfml.mcne.services.WebSocketEventPublisher;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class EmailNotificationStrategyTest {

  @Mock
  private SesClient sesClient;

  @Mock
  private WebSocketEventPublisher wsPublisher;

  private EmailNotificationStrategy strategy;

  @BeforeEach
  void setUp() {
    // demoMode = false: production path by default
    strategy = new EmailNotificationStrategy(sesClient, "no-reply@example.com", wsPublisher, false);
  }

  // ── supports() ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("supports() should return true for EMAIL channel")
  void shouldSupportEmailChannel() {
    assertThat(strategy.supports(NotificationChannel.EMAIL)).isTrue();
  }

  @Test
  @DisplayName("supports() should return false for non-EMAIL channels")
  void shouldNotSupportOtherChannels() {
    assertThat(strategy.supports(NotificationChannel.SMS)).isFalse();
  }

  // ── send() — production path ─────────────────────────────────────────────

  @Test
  @DisplayName("send() should call SES and emit SENT event on success")
  void shouldSendEmailAndEmitSentEvent() {
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenReturn(SendEmailResponse.builder().messageId("msg-1").build());

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!", NotificationChannel.EMAIL, Map.of());

    strategy.send(logId, request);

    verify(sesClient).sendEmail(any(SendEmailRequest.class));

    ArgumentCaptor<WebSocketNotificationEvent> captor =
        ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(wsPublisher).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.SENT);
    assertThat(captor.getValue().logId()).isEqualTo(logId);
    assertThat(captor.getValue().channel()).isEqualTo("EMAIL");
  }

  @Test
  @DisplayName("send() should emit RETRYING event and rethrow on SdkClientException")
  void shouldEmitRetryingEventAndRethrowOnTransientError() {
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenThrow(SdkClientException.builder().message("Network error").build());

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!", NotificationChannel.EMAIL, Map.of());

    assertThatThrownBy(() -> strategy.send(logId, request))
        .isInstanceOf(SdkClientException.class);

    ArgumentCaptor<WebSocketNotificationEvent> captor =
        ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(wsPublisher).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.RETRYING);
  }

  // ── send() — demo mode ───────────────────────────────────────────────────

  @Test
  @DisplayName("send() in demo mode should simulate success without calling SES")
  void shouldSimulateSuccessInDemoMode() {
    strategy = new EmailNotificationStrategy(sesClient, "no-reply@example.com", wsPublisher, true);

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!",
        NotificationChannel.EMAIL,
        Map.of("isVisualizerClient", "true"));

    strategy.send(logId, request);

    verifyNoInteractions(sesClient);

    ArgumentCaptor<WebSocketNotificationEvent> captor =
        ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(wsPublisher).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.SENT);
  }

  @Test
  @DisplayName("send() in demo mode with simulateError should throw without calling SES")
  void shouldSimulateErrorInDemoMode() {
    strategy = new EmailNotificationStrategy(sesClient, "no-reply@example.com", wsPublisher, true);

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!",
        NotificationChannel.EMAIL,
        Map.of("isVisualizerClient", "true", "simulateError", "true"));

    assertThatThrownBy(() -> strategy.send(logId, request))
        .isInstanceOf(SdkClientException.class)
        .hasMessageContaining("Simulated");

    verifyNoInteractions(sesClient);
  }

  @Test
  @DisplayName("send() in demo mode without isVisualizerClient flag should use real SES path")
  void shouldUseProdPathInDemoModeWithoutVisualizerFlag() {
    strategy = new EmailNotificationStrategy(sesClient, "no-reply@example.com", wsPublisher, true);
    when(sesClient.sendEmail(any(SendEmailRequest.class)))
        .thenReturn(SendEmailResponse.builder().messageId("msg-2").build());

    UUID logId = UUID.randomUUID();
    // metadata does NOT have isVisualizerClient=true
    NotificationRequest request = new NotificationRequest(
        "user@example.com", "Hello!", NotificationChannel.EMAIL, Map.of());

    strategy.send(logId, request);

    verify(sesClient).sendEmail(any(SendEmailRequest.class));
  }
}
