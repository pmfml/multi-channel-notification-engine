package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;

import org.springframework.stereotype.Component;

@Component
public class SmsNotificationStrategy implements NotificationStrategy {

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == NotificationChannel.SMS;
  }

  @Override
  public void send(NotificationRequest request) {
    // In the future, we will refer to the AWS SNS or Twilio API here.
    System.out.println("Sending SMS to: " + request.recipient());
    System.out.println("Message: " + request.message());
  }
}
