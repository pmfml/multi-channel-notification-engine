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
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.util.UUID;

/**
 * Strategy implementation for sending email notifications.
 * Integrates with AWS Simple Email Service (SES) to dispatch messages.
 *
 * <p>Demo behaviour (artificial delays and simulated errors) is only active
 * when the {@code demo} Spring profile is enabled.
 */
@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

  private final SesClient sesClient;
  private final String senderEmail;
  private final WebSocketEventPublisher wsPublisher;
  private final boolean demoMode;

  public EmailNotificationStrategy(SesClient sesClient,
      @Value("${aws.ses.verified-email}") String senderEmail,
      WebSocketEventPublisher wsPublisher,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    this.sesClient = sesClient;
    this.senderEmail = senderEmail;
    this.wsPublisher = wsPublisher;
    this.demoMode = demoMode;
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.EMAIL;
  }

  @Retryable(retryFor = SdkClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
  @Override
  public void send(UUID logId, NotificationRequest request) {
    log.info("Sending EMAIL to: {}", request.recipient());

    try {
      if (demoMode && request.metadata() != null
          && "true".equalsIgnoreCase(request.metadata().get("isVisualizerClient"))) {
        boolean simulateError = "true".equalsIgnoreCase(request.metadata().get("simulateError"));
        if (simulateError) {
          log.warn("Demo mode: simulating AWS SES error for EMAIL");
          throw SdkClientException.builder().message("Simulated AWS SES Error").build();
        }
        log.info("Demo mode: simulating successful EMAIL delivery");
        wsPublisher.publish(new WebSocketNotificationEvent(logId, NotificationEventType.SENT, NotificationChannel.EMAIL.name(), request.message()));
        return;
      }

      SendEmailRequest emailRequest = SendEmailRequest.builder()
          .source(senderEmail)
          .destination(Destination.builder().toAddresses(request.recipient()).build())
          .message(Message.builder()
              .subject(Content.builder().data("Notification from MCNE").build())
              .body(Body.builder().text(Content.builder().data(request.message()).build()).build())
              .build())
          .build();

      sesClient.sendEmail(emailRequest);
      log.info("EMAIL successfully sent via AWS SES to: {}", request.recipient());
      wsPublisher.publish(new WebSocketNotificationEvent(logId, NotificationEventType.SENT, NotificationChannel.EMAIL.name(), request.message()));
    } catch (SdkClientException e) {
      wsPublisher.publish(new WebSocketNotificationEvent(logId, NotificationEventType.RETRYING, NotificationChannel.EMAIL.name(), e.getMessage()));
      throw e;
    }
  }
}
