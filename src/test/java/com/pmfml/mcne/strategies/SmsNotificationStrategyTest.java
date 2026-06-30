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
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@ExtendWith(MockitoExtension.class)
class SmsNotificationStrategyTest {

  @Mock
  private SnsClient snsClient;

  @Mock
  private WebSocketEventPublisher wsPublisher;

  private SmsNotificationStrategy strategy;

  @BeforeEach
  void setUp() {
    // demoMode = false: production path by default
    strategy = new SmsNotificationStrategy(snsClient, wsPublisher, false);
  }

  // ── supports() ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("supports() should return true for SMS channel")
  void shouldSupportSmsChannel() {
    assertThat(strategy.supports(NotificationChannel.SMS)).isTrue();
  }

  @Test
  @DisplayName("supports() should return false for non-SMS channels")
  void shouldNotSupportOtherChannels() {
    assertThat(strategy.supports(NotificationChannel.EMAIL)).isFalse();
  }

  // ── send() — production path ─────────────────────────────────────────────

  @Test
  @DisplayName("send() should call SNS and emit SENT event on success")
  void shouldSendSmsAndEmitSentEvent() {
    when(snsClient.publish(any(PublishRequest.class)))
        .thenReturn(PublishResponse.builder().messageId("msg-1").build());

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "+5511999999999", "Your code is 1234", NotificationChannel.SMS, Map.of());

    strategy.send(logId, request);

    verify(snsClient).publish(any(PublishRequest.class));

    ArgumentCaptor<WebSocketNotificationEvent> captor =
        ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(wsPublisher).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.SENT);
    assertThat(captor.getValue().logId()).isEqualTo(logId);
    assertThat(captor.getValue().channel()).isEqualTo("SMS");
  }

  @Test
  @DisplayName("send() should emit RETRYING event and rethrow on SdkClientException")
  void shouldEmitRetryingEventAndRethrowOnTransientError() {
    when(snsClient.publish(any(PublishRequest.class)))
        .thenThrow(SdkClientException.builder().message("Network error").build());

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "+5511999999999", "Your code is 1234", NotificationChannel.SMS, Map.of());

    assertThatThrownBy(() -> strategy.send(logId, request))
        .isInstanceOf(SdkClientException.class);

    ArgumentCaptor<WebSocketNotificationEvent> captor =
        ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(wsPublisher).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.RETRYING);
  }

  // ── send() — demo mode ───────────────────────────────────────────────────

  @Test
  @DisplayName("send() in demo mode should simulate success without calling SNS")
  void shouldSimulateSuccessInDemoMode() {
    strategy = new SmsNotificationStrategy(snsClient, wsPublisher, true);

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "+5511999999999", "Hello!",
        NotificationChannel.SMS,
        Map.of("isVisualizerClient", "true"));

    strategy.send(logId, request);

    verifyNoInteractions(snsClient);

    ArgumentCaptor<WebSocketNotificationEvent> captor =
        ArgumentCaptor.forClass(WebSocketNotificationEvent.class);
    verify(wsPublisher).publish(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo(NotificationEventType.SENT);
  }

  @Test
  @DisplayName("send() in demo mode with simulateError should throw without calling SNS")
  void shouldSimulateErrorInDemoMode() {
    strategy = new SmsNotificationStrategy(snsClient, wsPublisher, true);

    UUID logId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        "+5511999999999", "Hello!",
        NotificationChannel.SMS,
        Map.of("isVisualizerClient", "true", "simulateError", "true"));

    assertThatThrownBy(() -> strategy.send(logId, request))
        .isInstanceOf(SdkClientException.class)
        .hasMessageContaining("Simulated");

    verifyNoInteractions(snsClient);
  }
}
