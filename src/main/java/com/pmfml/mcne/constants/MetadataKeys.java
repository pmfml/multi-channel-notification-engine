package com.pmfml.mcne.constants;

/**
 * Centralizes the metadata keys used across the notification pipeline.
 * Avoids scattered "magic strings" and makes typos compile-time detectable.
 */
public final class MetadataKeys {

  /** Marks a request originating from the demo Visualizer frontend. */
  public static final String IS_VISUALIZER_CLIENT = "isVisualizerClient";

  /** Artificial delay (in milliseconds) applied in demo mode. */
  public static final String DEMO_DELAY_MS = "demoDelayMs";

  /** When "true", forces a simulated provider error in demo mode. */
  public static final String SIMULATE_ERROR = "simulateError";

  /** Value used to identify the Visualizer client (via header and metadata). */
  public static final String VISUALIZER_CLIENT_VALUE = "Visualizer";

  /** HTTP header used by the Visualizer to identify itself. */
  public static final String CLIENT_HEADER = "X-MCNE-Client";

  private MetadataKeys() {
    // Utility class — prevent instantiation.
  }
}