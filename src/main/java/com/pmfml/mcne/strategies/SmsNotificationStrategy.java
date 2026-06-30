package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationEventType;
import com.pmfml.mcne.services.WebSocketEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.UUID;

/**
 * Strategy implementation for sending SMS notifications.
 * Integrates with AWS Simple Notification Service (SNS) to dispatch text messages.
 *
 * <p>Demo behaviour (artificial delays and simulated errors) is only active
 * when the {@code demo} Spring profile is enabled.
 */
@Slf4j
@Component
public class SmsNotificationStrategy implements NotificationStrategy {

  private final SnsClient snsClient;
  private final WebSocketEventPublisher wsPublisher;
  private final boolean demoMode;

  public SmsNotificationStrategy(SnsClient snsClient, WebSocketEventPublisher wsPublisher,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    this.snsClient = snsClient;
    this.wsPublisher = wsPublisher;
    this.demoMode = demoMode;
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }

  @Retryable(retryFor = SdkClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
  @Override
  public void send(UUID logId, NotificationRequest request) {
    log.info("Sending SMS to: {}", request.recipient());

    try {
      if (demoMode && request.metadata() != null
          && "true".equalsIgnoreCase(request.metadata().get("isVisualizerClient"))) {
        boolean simulateError = "true".equalsIgnoreCase(request.metadata().get("simulateError"));
        if (simulateError) {
          log.warn("Demo mode: simulating AWS SNS error for SMS");
          throw SdkClientException.builder().message("Simulated AWS SNS Error").build();
        }
        log.info("Demo mode: simulating successful SMS delivery");
        wsPublisher.publish(new WebSocketNotificationEvent(logId, NotificationEventType.SENT, NotificationChannel.SMS.name()));
        return;
      }

      PublishRequest publishRequest = PublishRequest.builder()
          .phoneNumber(request.recipient())
          .message(request.message())
          .build();

      snsClient.publish(publishRequest);
      log.info("SMS successfully sent via AWS SNS to: {}", request.recipient());
      wsPublisher.publish(new WebSocketNotificationEvent(logId, NotificationEventType.SENT, NotificationChannel.SMS.name()));
    } catch (SdkClientException e) {
      wsPublisher.publish(new WebSocketNotificationEvent(logId, NotificationEventType.RETRYING, NotificationChannel.SMS.name()));
      throw e;
    }
  }
}
