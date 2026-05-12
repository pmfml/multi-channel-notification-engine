package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;

public interface NotificationStrategy {

  /**
   * Método para o serviço descobrir se esta estratégia suporta o canal solicitado
   */
  boolean supports(NotificationChannel channel);

  /**
   * Método que efetivamente fará o envio
   */
  void send(NotificationRequest request);
}
