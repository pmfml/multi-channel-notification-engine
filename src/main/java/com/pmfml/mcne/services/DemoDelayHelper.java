package com.pmfml.mcne.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pmfml.mcne.constants.MetadataKeys;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies artificial delays for demo/visualizer purposes.
 *
 * <p>
 * All logic is a no-op unless the {@code demo} profile is active and the
 * request originates from the Visualizer client. Centralizing this here removes
 * the previously duplicated {@code applyDemoDelay} logic from the dispatcher
 * and
 * the consumer.
 */
@Slf4j
@Component
public class DemoDelayHelper {

  private final boolean demoMode;

  public DemoDelayHelper(@Value("#{environment.acceptsProfiles('demo')}") boolean demoMode) {
    this.demoMode = demoMode;
  }

  /**
   * Sleeps for the delay declared in the request metadata, when applicable.
   * No-op when the demo profile is inactive, when the request is not from the
   * Visualizer, or when no valid delay is present.
   *
   * @param metadata the request metadata (may be {@code null})
   */
  public void applyDelay(Map<String, String> metadata) {
    if (!demoMode) {
      return;
    }
    if (metadata == null || !metadata.containsKey(MetadataKeys.DEMO_DELAY_MS)) {
      return;
    }
    if (!"true".equalsIgnoreCase(metadata.get(MetadataKeys.IS_VISUALIZER_CLIENT))) {
      return;
    }
    try {
      long delay = Long.parseLong(metadata.get(MetadataKeys.DEMO_DELAY_MS));
      if (delay > 0) {
        log.debug("Demo mode: applying artificial delay of {} ms for Visualizer", delay);
        Thread.sleep(delay);
      }
    } catch (InterruptedException e) {
      log.warn("Demo delay interrupted; restoring interrupt flag");
      Thread.currentThread().interrupt();
    } catch (NumberFormatException ignored) {
      log.warn("Demo mode: malformed '{}' metadata value '{}' — skipping delay",
          MetadataKeys.DEMO_DELAY_MS, metadata.get(MetadataKeys.DEMO_DELAY_MS));
    }
  }
}