package com.pmfml.mcne.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationStatus;
import com.pmfml.mcne.producers.NotificationProducer;
import com.pmfml.mcne.strategies.NotificationStrategy;
import com.pmfml.mcne.services.WebSocketEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceTest {

        @Mock
        private NotificationStrategy mockStrategy;

        @Mock
        private NotificationLogService notificationLogService;

        @Mock
        private NotificationProducer producer;

        @Mock
        private WebSocketEventPublisher wsPublisher;

        private NotificationDispatcherService service;

        @BeforeEach
        void setUp() {
                // Injected the mock strategy into the list that the Dispatcher expects.
                service = new NotificationDispatcherService(List.of(mockStrategy), notificationLogService, producer, wsPublisher);
        }

        @Test
        @DisplayName("Should successfully dispatch notification to queue")
        void shouldDispatchToQueueSuccessfully() {
                NotificationRequest request = mock(NotificationRequest.class);
                when(request.channel()).thenReturn(NotificationChannel.EMAIL);
                when(mockStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);

                when(request.message()).thenReturn("Test Message");
                NotificationLog log = NotificationLog.builder().id(UUID.randomUUID()).build();
                when(notificationLogService.savePendingLog(request)).thenReturn(log);

                service.dispatchToQueue(request);

                verify(notificationLogService).savePendingLog(request);
                verify(producer).publish(any(NotificationEvent.class));
                verify(wsPublisher).publish(any());
        }

        @Test
        @DisplayName("Should throw exception when channel is unsupported during dispatch")
        void shouldThrowExceptionWhenUnsupportedChannel() {
                NotificationRequest request = mock(NotificationRequest.class);
                when(request.channel()).thenReturn(NotificationChannel.EMAIL);
                when(mockStrategy.supports(NotificationChannel.EMAIL)).thenReturn(false);

                assertThatThrownBy(() -> service.dispatchToQueue(request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Unsupported notification channel");

                verify(notificationLogService, never()).savePendingLog(any());
        }

        @Test
        @DisplayName("Should handle exception and route to DLQ when process from queue fails")
        void shouldHandleExceptionDuringProcessFromQueue() {
                NotificationRequest request = mock(NotificationRequest.class);
                when(request.channel()).thenReturn(NotificationChannel.EMAIL);

                NotificationEvent event = mock(NotificationEvent.class);
                when(event.request()).thenReturn(request);
                UUID logId = UUID.randomUUID();
                when(event.logId()).thenReturn(logId);

                when(mockStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);
                doThrow(new RuntimeException("3rd Party API Down")).when(mockStrategy).send(request);

                assertThatThrownBy(() -> service.processFromQueue(event))
                                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                                .hasMessageContaining("Routing to DLQ");

                // The status in the database should be FAILED.
                verify(notificationLogService).updateStatus(logId, NotificationStatus.FAILED);
        }
}