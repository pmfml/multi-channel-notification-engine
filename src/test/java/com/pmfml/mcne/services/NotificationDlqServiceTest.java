package com.pmfml.mcne.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.List;
import java.util.Map;

import com.pmfml.mcne.config.RabbitMQConfig;

@ExtendWith(MockitoExtension.class)
class NotificationDlqServiceTest {

  @Mock
  private RabbitTemplate rabbitTemplate;

  @InjectMocks
  private NotificationDlqService service;

  @Test
  @DisplayName("Should pull messages from DLQ and route them back to the main exchange")
  void shouldReprocessMessages() {
    // Simulate that the DLQ has 2 messages, and then it becomes empty (null).
    Message mockMessage1 = mock(Message.class);
    Message mockMessage2 = mock(Message.class);

    when(rabbitTemplate.receive(RabbitMQConfig.NOTIFICATION_DLQ))
        .thenReturn(mockMessage1)
        .thenReturn(mockMessage2)
        .thenReturn(null);

    int count = service.reprocessMessages();

    assertThat(count).isEqualTo(2);

    // Check if both messages were returned to the main exchange.
    verify(rabbitTemplate, times(2))
        .send(eq(RabbitMQConfig.NOTIFICATION_EXCHANGE), eq(RabbitMQConfig.NOTIFICATION_ROUTING_KEY),
            any(Message.class));
  }

  @Test
  @DisplayName("Should discard poison messages exceeding max retries")
  void shouldDiscardPoisonMessages() {
    Message mockMessage = mock(Message.class);
    MessageProperties mockProps = mock(MessageProperties.class);
    
    lenient().when(mockMessage.getMessageProperties()).thenReturn(mockProps);
    
    Map<String, Object> xDeathProps = Map.of("count", 3L);
    List<Map<String, Object>> xDeathList = List.of(xDeathProps);
    Map<String, Object> headers = Map.of("x-death", xDeathList);
    
    lenient().when(mockProps.getHeaders()).thenReturn(headers);
    
    when(rabbitTemplate.receive(RabbitMQConfig.NOTIFICATION_DLQ))
        .thenReturn(mockMessage)
        .thenReturn(null);

    int count = service.reprocessMessages();

    assertThat(count).isEqualTo(0);
    verify(rabbitTemplate, never()).send(eq(RabbitMQConfig.NOTIFICATION_EXCHANGE), anyString(), any(Message.class));
  }
}