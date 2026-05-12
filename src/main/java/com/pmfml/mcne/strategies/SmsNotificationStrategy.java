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
    // No futuro, aqui chamaremos a API do AWS SNS ou Twilio
    System.out.println("Enviando SMS para: " + request.recipient());
    System.out.println("Mensagem: " + request.message());
  }
}
