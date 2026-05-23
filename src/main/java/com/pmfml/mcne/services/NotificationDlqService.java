package com.pmfml.mcne.services;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.pmfml.mcne.config.RabbitMQConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationDlqService {

  private final RabbitTemplate rabbitTemplate;

  public NotificationDlqService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public int reprocessMessages() {
    int count = 0;
    Message message;

    // Pull messages from the DLQ one by one until the queue is empty (returns
    // null).
    while ((message = rabbitTemplate.receive(RabbitMQConfig.NOTIFICATION_DLQ)) != null) {
      log.info("Reprocessing message from DLQ");
      rabbitTemplate.send(RabbitMQConfig.NOTIFICATION_EXCHANGE, RabbitMQConfig.NOTIFICATION_ROUTING_KEY, message);
      count++;
    }

    return count;
  }
}