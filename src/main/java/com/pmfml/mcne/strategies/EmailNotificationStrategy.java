package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.EMAIL;
  }

  @Override
  public void send(NotificationRequest request) {
    // In the future, we will refer to the AWS SES or SendGrid API here.
    System.out.println("Sending EMAIL to: " + request.recipient());
    System.out.println("Message: " + request.message());
  }
}
