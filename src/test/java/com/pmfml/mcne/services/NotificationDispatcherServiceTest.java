package com.pmfml.mcne.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
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
        dispatcher = new NotificationDispatcherService(List.of(emailStrategy, smsStrategy), notificationLogService,
                producer);
    }

    @Test
    void shouldProcessMessageFromQueueSuccessfully() {
        NotificationRequest request = new NotificationRequest(
                "test@example.com",
                "Test Message",
                NotificationChannel.EMAIL,
                Map.of());

        when(emailStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);

        dispatcher.processFromQueue(request);

        verify(emailStrategy, times(1)).send(request);
        verify(smsStrategy, never()).send(any());
    }

    @Test
    void shouldThrowExceptionWhenProcessingUnsupportedChannel() {
        NotificationRequest request = new NotificationRequest(
                "user",
                "Msg",
                NotificationChannel.PUSH,
                Map.of());

        when(emailStrategy.supports(NotificationChannel.PUSH)).thenReturn(false);
        when(smsStrategy.supports(NotificationChannel.PUSH)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> dispatcher.processFromQueue(request));

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

        when(emailStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);

        dispatcher.dispatchToQueue(request);

        verify(notificationLogService, times(1)).savePendingLog(request);
        verify(producer, times(1)).publish(request);
    }
}
