package com.pmfml.mcne.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pmfml.mcne.dtos.NotificationEvent;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.entities.NotificationLog;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationStatus;
import com.pmfml.mcne.producers.NotificationProducer;
import com.pmfml.mcne.strategies.NotificationStrategy;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceTest {

        @Mock
        private NotificationStrategy emailStrategy;

        @Mock
        private NotificationStrategy smsStrategy;

        @Mock
        private NotificationLogService notificationLogService;

        @Mock
        private NotificationProducer producer;

        private NotificationDispatcherService dispatcher;

        @BeforeEach
        void setUp() {
                dispatcher = new NotificationDispatcherService(List.of(emailStrategy, smsStrategy),
                                notificationLogService,
                                producer);
        }

        @Test
        void shouldProcessMessageFromQueueSuccessfully() {
                UUID logId = UUID.randomUUID();
                NotificationRequest request = new NotificationRequest(
                                "test@example.com",
                                "Test Message",
                                NotificationChannel.EMAIL,
                                Map.of());
                NotificationEvent event = new NotificationEvent(logId, request);

                when(emailStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);

                dispatcher.processFromQueue(event);

                verify(emailStrategy, times(1)).send(request);
                verify(smsStrategy, never()).send(any());
                verify(notificationLogService, times(1)).updateStatus(logId, NotificationStatus.SENT);
        }

        @Test
        void shouldThrowExceptionWhenProcessingUnsupportedChannel() {
                NotificationRequest request = new NotificationRequest(
                                "user",
                                "Msg",
                                NotificationChannel.PUSH,
                                Map.of());
                NotificationEvent event = new NotificationEvent(UUID.randomUUID(), request);

                when(emailStrategy.supports(NotificationChannel.PUSH)).thenReturn(false);
                when(smsStrategy.supports(NotificationChannel.PUSH)).thenReturn(false);

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class, () -> dispatcher.processFromQueue(event));

                assertEquals(
                                "Unsupported notification channel: PUSH", exception.getMessage());
        }

        @Test
        void shouldDispatchMessageToQueueSuccessfully() {
                NotificationRequest request = new NotificationRequest(
                                "test@example.com",
                                "Test Message",
                                NotificationChannel.EMAIL,
                                Map.of());

                NotificationLog mockLog = mock(NotificationLog.class);
                when(mockLog.getId()).thenReturn(UUID.randomUUID());

                when(emailStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);
                when(notificationLogService.savePendingLog(request)).thenReturn(mockLog);

                dispatcher.dispatchToQueue(request);

                verify(notificationLogService, times(1)).savePendingLog(request);
                verify(producer, times(1)).publish(any(NotificationEvent.class));
        }
}
