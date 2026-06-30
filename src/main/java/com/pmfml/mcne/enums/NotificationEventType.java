package com.pmfml.mcne.enums;

/**
 * Represents the possible lifecycle states of a notification event
 * broadcast via WebSocket to connected clients.
 *
 * <p>Events flow in the order: RECEIVED -> QUEUED -> PROCESSING -> SENT (happy path),
 * or -> RETRYING -> SENT / DLQ (on transient / persistent failure).
 */
public enum NotificationEventType {
  /** HTTP request accepted by the REST layer; log entry created. */
  RECEIVED,
  /** Notification published to RabbitMQ. */
  QUEUED,
  /** Worker thread picked up the message from the queue. */
  PROCESSING,
  /** Transient error caught; Spring Retry will attempt delivery again. */
  RETRYING,
  /** Delivery confirmed by the external provider (AWS SES / SNS). */
  SENT,
  /** All retry attempts exhausted; message routed to the Dead Letter Queue. */
  DLQ
}
