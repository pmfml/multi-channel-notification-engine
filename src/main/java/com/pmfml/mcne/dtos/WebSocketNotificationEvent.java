package com.pmfml.mcne.dtos;

import com.pmfml.mcne.enums.NotificationEventType;

import java.util.UUID;

/**
 * DTO representing a lifecycle state change in the notification pipeline,
 * broadcast to connected frontend clients via WebSocket.
 *
 * <p>Privacy note: the {@code message} field is only broadcast when the
 * {@code demo} profile is active (so the Visualizer terminal can display it).
 * In production, {@link com.pmfml.mcne.services.WebSocketEventPublisher} strips
 * this field before sending, to avoid broadcasting potentially sensitive
 * message content over the shared topic.
 */
public record WebSocketNotificationEvent(
    UUID logId,
    NotificationEventType eventType,
    String channel,
    String message
) {
}
