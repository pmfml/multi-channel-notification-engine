package com.pmfml.mcne.producers;

import com.pmfml.mcne.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.dtos.NotificationEvent;

@Component
public class NotificationProducer {

  private final RabbitTemplate rabbitTemplate;

  public NotificationProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(NotificationEvent event) {
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.NOTIFICATION_EXCHANGE,
        RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
        event);
  }
}
