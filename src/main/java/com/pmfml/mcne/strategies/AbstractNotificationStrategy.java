package com.pmfml.mcne.strategies;

import java.util.UUID;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import com.pmfml.mcne.constants.MetadataKeys;
import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.dtos.WebSocketNotificationEvent;
import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationEventType;
import com.pmfml.mcne.services.WebSocketEventPublisher;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Template Method base class shared by every notification channel strategy.
 *
 * <p>
 * This class implements the <b>Template Method</b> design pattern: it defines
 * the invariant skeleton of the delivery algorithm once — retry policy,
 * demo-mode
 * simulation, structured logging and the emission of {@code SENT} /
 * {@code RETRYING}
 * WebSocket events — while delegating the two variant steps to concrete
 * subclasses:
 *
 * <ul>
 * <li>{@link #channel()} — which {@link NotificationChannel} this strategy
 * handles;</li>
 * <li>{@link #doSend(NotificationRequest)} — the provider-specific delivery
 * call
 * (e.g. AWS SES for email, AWS SNS for SMS);</li>
 * <li>{@link #providerName()} — a human-readable provider label used in
 * logs.</li>
 * </ul>
 *
 * <p>
 * Centralizing these cross-cutting concerns keeps each concrete strategy
 * focused
 * solely on its provider integration, honouring both the Single Responsibility
 * and
 * Open/Closed principles: adding a new channel means implementing three small
 * methods,
 * with retry, logging and event-broadcasting behaviour inherited for free.
 *
 * <p>
 * <b>Resiliency:</b> {@link #send(UUID, NotificationRequest)} is annotated with
 * {@link Retryable} so transient {@link SdkClientException}s (network/timeout
 * errors)
 * are retried up to 3 times with exponential backoff (2s, then 4s). Permanent
 * errors
 * are not retried and propagate so the message is routed to the Dead Letter
 * Queue.
 *
 * <p>
 * <b>Demo mode:</b> when the {@code demo} Spring profile is active and the
 * request
 * originates from the Visualizer client, delivery is simulated (no real
 * provider call),
 * optionally forcing an error via the {@code simulateError} metadata flag. This
 * entire
 * path is inert in production.
 */
@Slf4j
public abstract class AbstractNotificationStrategy implements NotificationStrategy {

  protected final WebSocketEventPublisher wsPublisher;
  protected final boolean demoMode;

  protected AbstractNotificationStrategy(WebSocketEventPublisher wsPublisher, boolean demoMode) {
    this.wsPublisher = wsPublisher;
    this.demoMode = demoMode;
  }

  /**
   * @return the notification channel handled by this strategy.
   */
  protected abstract NotificationChannel channel();

  /**
   * @return a human-readable name of the underlying provider (e.g.
   *         {@code "AWS SES"},
   *         {@code "AWS SNS"}), used to produce descriptive log messages.
   */
  protected abstract String providerName();

  /**
   * Performs the actual provider-specific delivery (e.g. the AWS SES / SNS SDK
   * call).
   * Called only on the real (non-demo) delivery path.
   *
   * @param request the notification payload
   */
  protected abstract void doSend(NotificationRequest request);

  @Override
  public boolean supports(NotificationChannel channel) {
    return channel == channel();
  }

  /**
   * Delivers the notification, applying the shared retry policy and broadcasting
   * lifecycle events. On success emits {@code SENT}; on a transient error emits
   * {@code RETRYING} and rethrows so Spring Retry can attempt again.
   *
   * @param logId   the notification log identifier (correlates DB, queue and WS
   *                events)
   * @param request the notification payload
   */
  @Retryable(retryFor = SdkClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
  @Override
  public void send(UUID logId, NotificationRequest request) {
    log.info("Sending {} to: {}", channel(), request.recipient());

    try {
      // Demo/Visualizer path: simulate delivery without touching the real provider.
      if (isDemoRequest(request)) {
        handleDemoDelivery(logId, request);
        return;
      }

      // Production path: perform the real provider call (AWS SES / SNS).
      doSend(request);
      log.info("{} successfully sent via {} to: {}", channel(), providerName(), request.recipient());
      publish(logId, NotificationEventType.SENT, request.message());
    } catch (SdkClientException e) {
      // Transient error — emit RETRYING and rethrow so @Retryable can back off and
      // retry.
      log.warn("Transient error sending {} via {} to {}: {}. Will retry.",
          channel(), providerName(), request.recipient(), e.getMessage());
      publish(logId, NotificationEventType.RETRYING, e.getMessage());
      throw e;
    }
  }

  /**
   * @return {@code true} only when running under the {@code demo} profile and the
   *         request was flagged as originating from the Visualizer client.
   */
  private boolean isDemoRequest(NotificationRequest request) {
    return demoMode
        && request.metadata() != null
        && "true".equalsIgnoreCase(request.metadata().get(MetadataKeys.IS_VISUALIZER_CLIENT));
  }

  /**
   * Simulates a delivery for the Visualizer. Honours the {@code simulateError}
   * flag
   * to deliberately trigger the retry/DLQ flow without any real provider
   * interaction.
   */
  private void handleDemoDelivery(UUID logId, NotificationRequest request) {
    boolean simulateError = "true".equalsIgnoreCase(request.metadata().get(MetadataKeys.SIMULATE_ERROR));
    if (simulateError) {
      log.warn("Demo mode: simulating {} error for {}", providerName(), channel());
      throw SdkClientException.builder().message("Simulated " + providerName() + " Error").build();
    }
    log.info("Demo mode: simulating successful {} delivery", channel());
    publish(logId, NotificationEventType.SENT, request.message());
  }

  /**
   * Broadcasts a lifecycle event to the WebSocket topic for the Visualizer,
   * always tagged with this strategy's channel.
   */
  private void publish(UUID logId, NotificationEventType eventType, String message) {
    wsPublisher.publish(new WebSocketNotificationEvent(logId, eventType, channel().name(), message));
  }
}