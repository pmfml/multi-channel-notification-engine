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
import com.pmfml.mcne.strategies.NotificationStrategy;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceTest {

    // Mockamos as dependências. Não queremos a implementação real de Email/SMS aqui.
    @Mock
    private NotificationStrategy emailStrategy;

    @Mock
    private NotificationStrategy smsStrategy;

    // A classe que realmente vamos testar
    private NotificationDispatcherService dispatcher;

    @BeforeEach
    void setUp() {
        // Arrange global: Injetamos os mocks na nossa classe alvo
        dispatcher = new NotificationDispatcherService(List.of(emailStrategy, smsStrategy));
    }

    @Test
    void shouldDispatchToCorrectStrategyWhenChannelIsSupported() {
        // Arrange (Preparação)
        NotificationRequest request = new NotificationRequest(
                "test@example.com",
                "Test Message",
                NotificationChannel.EMAIL,
                Map.of()
        );

        // Ensinamos o nosso mock a responder: Quando te perguntarem se suporta EMAIL, diga "true".
        when(emailStrategy.supports(NotificationChannel.EMAIL)).thenReturn(true);

        // Act (Ação)
        dispatcher.dispatch(request);

        // Assert (Verificação)
        // Verificamos se o método send() da estratégia de email foi chamado exatamente 1 vez
        verify(emailStrategy, times(1)).send(request);
        
        // Garantimos que a estratégia de SMS nunca foi chamada (para evitar bugs de enviar 2x)
        verify(smsStrategy, never()).send(any());
    }

    @Test
    void shouldThrowExceptionWhenChannelIsNotSupported() {
        // Arrange
        NotificationRequest request = new NotificationRequest(
            "user", 
            "Msg", 
            NotificationChannel.PUSH, 
            Map.of());

        when(emailStrategy.supports(NotificationChannel.PUSH)).thenReturn(false);

        when(smsStrategy.supports(NotificationChannel.PUSH)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> dispatcher.dispatch(request));

        assertEquals(
            "Canal de notificação não suportado: PUSH", exception.getMessage());
    }
}
