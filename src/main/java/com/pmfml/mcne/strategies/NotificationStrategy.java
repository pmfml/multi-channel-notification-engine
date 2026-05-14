package com.pmfml.mcne.strategies;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;

public interface NotificationStrategy {

  /**
   * Determines if the current strategy supports the specified notification
   * channel.
   *
   * @param channel the notification channel to evaluate
   * @return true if the strategy supports the given channel, false otherwise
   */
  boolean supports(NotificationChannel channel);

  /**
   * Executes the delivery of the notification request.
   *
   * @param request the payload containing the notification details
   */
  void send(NotificationRequest request);
}
