package com.pmfml.mcne.producers;

import com.pmfml.mcne.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.dtos.NotificationRequest;

@Component
public class NotificationProducer {

  private final RabbitTemplate template;

  public NotificationProducer(RabbitTemplate template) {
    this.template = template;
  }

  public void publish(NotificationRequest request) {
    template.convertAndSend(
        RabbitMQConfig.NOTIFICATION_EXCHANGE,
        RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
        request);
  }
}
