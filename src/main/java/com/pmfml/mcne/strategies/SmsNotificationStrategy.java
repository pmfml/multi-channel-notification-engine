package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.services.WebSocketEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Strategy implementation for sending SMS notifications.
 * Integrates with AWS Simple Notification Service (SNS) to dispatch text
 * messages.
 *
 * <p>
 * Demo behaviour (artificial delays and simulated errors) is only active
 * when the {@code demo} Spring profile is enabled.
 */
@Slf4j
@Component
public class SmsNotificationStrategy extends AbstractNotificationStrategy {

  private final SnsClient snsClient;

  public SmsNotificationStrategy(SnsClient snsClient, WebSocketEventPublisher wsPublisher,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    super(wsPublisher, demoMode);
    this.snsClient = snsClient;
  }

  @Override
  protected NotificationChannel channel() {
    return NotificationChannel.SMS;
  }

  @Override
  protected String providerName() {
    return "AWS SNS";
  }

  @Override
  protected void doSend(NotificationRequest request) {
    PublishRequest publishRequest = PublishRequest.builder()
        .phoneNumber(request.recipient())
        .message(request.message())
        .build();

    snsClient.publish(publishRequest);
  }
}
