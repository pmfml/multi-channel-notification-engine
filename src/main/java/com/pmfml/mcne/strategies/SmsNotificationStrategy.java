package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;

import jakarta.persistence.criteria.CriteriaBuilder.In;

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
    System.out.println("Enviando SMS para: " + request.recipient());
    System.out.println("Mensagem: " + request.message());
  }
}
