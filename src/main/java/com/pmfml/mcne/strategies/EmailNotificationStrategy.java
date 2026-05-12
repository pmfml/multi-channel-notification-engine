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
    // No futuro, aqui chamaremos a API do AWS SES ou SendGrid
    System.out.println("Enviando EMAIL para: " + request.recipient());
    System.out.println("Mensagem: " + request.message());
  }
}
