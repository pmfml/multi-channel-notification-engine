package com.pmfml.mcne.services;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.pmfml.mcne.config.RabbitMQConfig;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for handling operations related to the Dead Letter Queue (DLQ).
 */
@Slf4j
@Service
public class NotificationDlqService {

  private static final int MAX_DLQ_RETRIES = 3;
  private final RabbitTemplate rabbitTemplate;

  public NotificationDlqService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Pulls all pending messages from the DLQ and routes them back to the main exchange
   * for reprocessing. Discards "poison messages" that exceed max retries.
   *
   * @return the total number of messages reprocessed
   */
  public int reprocessMessages() {
    int count = 0;
    Message message;

    // Pull messages from the DLQ one by one until the queue is empty (returns null).
    while ((message = rabbitTemplate.receive(RabbitMQConfig.NOTIFICATION_DLQ)) != null) {
      if (isPoisonMessage(message)) {
        log.error("Message exceeded max DLQ retries ({}). Discarding poison message.", MAX_DLQ_RETRIES);
        continue; // Drop the message without republishing
      }

      log.info("Reprocessing message from DLQ");
      rabbitTemplate.send(RabbitMQConfig.NOTIFICATION_EXCHANGE, RabbitMQConfig.NOTIFICATION_ROUTING_KEY, message);
      count++;
    }

    return count;
  }

  @SuppressWarnings("unchecked")
  private boolean isPoisonMessage(Message message) {
    if (message.getMessageProperties() == null) {
      return false;
    }
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    if (headers == null || !headers.containsKey("x-death")) {
      return false;
    }

    try {
      List<Map<String, ?>> xDeathList = (List<Map<String, ?>>) headers.get("x-death");
      if (xDeathList != null && !xDeathList.isEmpty()) {
        Map<String, ?> xDeathProps = xDeathList.get(0);
        Long count = (Long) xDeathProps.get("count");
        if (count != null && count >= MAX_DLQ_RETRIES) {
          return true;
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse x-death headers", e);
    }
    return false;
  }
}