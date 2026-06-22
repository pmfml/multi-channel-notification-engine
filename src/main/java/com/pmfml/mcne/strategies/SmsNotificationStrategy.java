package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
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

  public SmsNotificationStrategy(SnsClient snsClient) {
    this.snsClient = snsClient;
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }

  @Retryable(retryFor = SdkClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
  @Override
  public void send(NotificationRequest request) {
    log.info("Sending SMS to: {}", request.recipient());
    log.info("Message: {}", request.message());

    PublishRequest publishRequest = PublishRequest.builder()
        .phoneNumber(request.recipient())
        .message(request.message())
        .build();

    snsClient.publish(publishRequest);
    log.info("SMS successfully sent via AWS SNS!");
  }
}
