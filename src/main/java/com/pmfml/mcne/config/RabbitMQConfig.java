package com.pmfml.mcne.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String NOTIFICATION_QUEUE = "notification.queue";
  public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
  public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

  @Bean
  public Queue notificationQueue() {
    // true = durable (the queue will survive a broker restart)
    return new Queue(NOTIFICATION_QUEUE, true);
  }

  @Bean
  public DirectExchange notificationExchange() {
    return new DirectExchange(NOTIFICATION_EXCHANGE);
  }

  @Bean
  public Binding binding(Queue notificationQueue, DirectExchange notificationExchange) {
    return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
  }

  /**
   * Configures JSON serialization for our RabbitMQ messages so we don't send raw
   * bytes.
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * Customizes the RabbitTemplate to use our JSON converter.
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }
}