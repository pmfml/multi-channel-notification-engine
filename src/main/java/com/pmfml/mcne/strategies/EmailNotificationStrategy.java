package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
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

/**
 * Strategy implementation for sending email notifications.
 * Integrates with AWS Simple Email Service (SES) to dispatch messages.
 */
@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

  private final SesClient sesClient;
  private final String senderEmail;

  public EmailNotificationStrategy(SesClient sesClient, @Value("${aws.ses.verified-email}") String senderEmail) {
    this.sesClient = sesClient;
    this.senderEmail = senderEmail;
  }

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.EMAIL;
  }

  @Retryable(retryFor = SdkClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
  @Override
  public void send(NotificationRequest request) {
    log.info("Sending EMAIL to: {}", request.recipient());
    log.info("Message: {}", request.message());

    SendEmailRequest emailRequest = SendEmailRequest.builder()
        .source(senderEmail)
        .destination(Destination.builder().toAddresses(request.recipient()).build())
        .message(Message.builder()
            .subject(Content.builder().data("Notification from MCNE").build())
            .body(Body.builder().text(Content.builder().data(request.message()).build()).build())
            .build())
        .build();

    sesClient.sendEmail(emailRequest);
    log.info("EMAIL successfully sent via AWS SES!");
  }
}
