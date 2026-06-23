package com.pmfml.mcne.dtos;

import java.util.UUID;

/**
 * DTO representing an event state change in the notification lifecycle,
 * meant to be broadcasted to the frontend visualizer via WebSockets.
 */
public record WebSocketNotificationEvent(
    UUID logId,
    String eventType, // e.g., RECEIVED, QUEUED, PROCESSING, RETRYING, SENT, FAILED, DLQ
    String channel,
    String message
) {
}
