package com.pmfml.mcne.dtos;

import com.pmfml.mcne.enums.NotificationEventType;

import java.util.UUID;

/**
 * DTO representing a lifecycle state change in the notification pipeline,
 * broadcast to connected frontend clients via WebSocket.
 *
 * <p>Privacy note: this record intentionally omits the recipient address and
 * message body to avoid broadcasting PII over the shared WebSocket topic.
 */
public record WebSocketNotificationEvent(
    UUID logId,
    NotificationEventType eventType,
    String channel
) {
}
