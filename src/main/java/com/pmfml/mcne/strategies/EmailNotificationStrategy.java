package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.services.WebSocketEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

/**
 * Strategy implementation for sending email notifications.
 * Integrates with AWS Simple Email Service (SES) to dispatch messages.
 *
 * <p>
 * Demo behaviour (artificial delays and simulated errors) is only active
 * when the {@code demo} Spring profile is enabled.
 */
@Slf4j
@Component
public class EmailNotificationStrategy extends AbstractNotificationStrategy {

  private final SesClient sesClient;
  private final String senderEmail;

  public EmailNotificationStrategy(SesClient sesClient,
      @Value("${aws.ses.verified-email}") String senderEmail,
      WebSocketEventPublisher wsPublisher,
      @Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    super(wsPublisher, demoMode);
    this.sesClient = sesClient;
    this.senderEmail = senderEmail;
  }

  @Override
  protected NotificationChannel channel() {
    return NotificationChannel.EMAIL;
  }

  @Override
  protected String providerName() {
    return "AWS SES";
  }

  @Override
  protected void doSend(NotificationRequest request) {
    SendEmailRequest emailRequest = SendEmailRequest.builder()
        .source(senderEmail)
        .destination(Destination.builder().toAddresses(request.recipient()).build())
        .message(Message.builder()
            .subject(Content.builder().data("Notification from MCNE").build())
            .body(Body.builder().text(Content.builder().data(request.message()).build()).build())
            .build())
        .build();

    sesClient.sendEmail(emailRequest);
  }
}
