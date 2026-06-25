package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.services.WebSocketEventPublisher;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Strategy implementation for sending SMS notifications.
 * Integrates with AWS Simple Notification Service (SNS) to dispatch text
 * messages.
 */
@Slf4j
@Component
public class SmsNotificationStrategy implements NotificationStrategy {

  private final SnsClient snsClient;
  private final WebSocketEventPublisher wsPublisher;

  public SmsNotificationStrategy(SnsClient snsClient, WebSocketEventPublisher wsPublisher) {
    this.snsClient = snsClient;
    this.wsPublisher = wsPublisher;
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }

  @Retryable(retryFor = SdkClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
  @Override
  public void send(java.util.UUID logId, NotificationRequest request) {
    log.info("Sending SMS to: {}", request.recipient());
    log.info("Message: {}", request.message());

    try {
      boolean isVisualizer = request.metadata() != null && "true".equalsIgnoreCase(request.metadata().get("isVisualizerClient"));
      boolean simulateError = request.metadata() != null && "true".equalsIgnoreCase(request.metadata().get("simulateError"));

      if (isVisualizer) {
        if (simulateError) {
          log.warn("Simulating AWS Error for SMS strategy");
          throw SdkClientException.builder().message("Simulated AWS SNS Error").build();
        } else {
          log.info("Simulating successful SMS delivery via AWS SNS for Visualizer!");
          wsPublisher.publish(new WebSocketNotificationEvent(logId, "SENT", "SMS", request.message()));
          return;
        }
      }

      PublishRequest publishRequest = PublishRequest.builder()
          .phoneNumber(request.recipient())
          .message(request.message())
          .build();

      snsClient.publish(publishRequest);
      log.info("SMS successfully sent via AWS SNS!");
      wsPublisher.publish(new WebSocketNotificationEvent(logId, "SENT", "SMS", request.message()));
    } catch (SdkClientException e) {
      wsPublisher.publish(new WebSocketNotificationEvent(logId, "RETRYING", "SMS", e.getMessage()));
      throw e;
    }
  }
}
