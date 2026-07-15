package com.pmfml.mcne.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pmfml.mcne.constants.MetadataKeys;

class DemoDelayHelperTest {

  @Test
  @DisplayName("Should not apply delay when demo mode is false")
  void shouldNotDelayWhenDemoModeIsFalse() {
    DemoDelayHelper helper = new DemoDelayHelper(false);
    
    long startTime = System.currentTimeMillis();
    helper.applyDelay(Map.of(
        MetadataKeys.DEMO_DELAY_MS, "500",
        MetadataKeys.IS_VISUALIZER_CLIENT, "true"
    ));
    long endTime = System.currentTimeMillis();
    
    assertThat(endTime - startTime).isLessThan(100); // Should return immediately
  }

  @Test
  @DisplayName("Should not apply delay when metadata is null")
  void shouldNotDelayWhenMetadataIsNull() {
    DemoDelayHelper helper = new DemoDelayHelper(true);
    
    long startTime = System.currentTimeMillis();
    helper.applyDelay(null);
    long endTime = System.currentTimeMillis();
    
    assertThat(endTime - startTime).isLessThan(100);
  }

  @Test
  @DisplayName("Should not apply delay when DEMO_DELAY_MS key is missing")
  void shouldNotDelayWhenDelayKeyMissing() {
    DemoDelayHelper helper = new DemoDelayHelper(true);
    
    long startTime = System.currentTimeMillis();
    helper.applyDelay(Map.of(MetadataKeys.IS_VISUALIZER_CLIENT, "true"));
    long endTime = System.currentTimeMillis();
    
    assertThat(endTime - startTime).isLessThan(100);
  }

  @Test
  @DisplayName("Should not apply delay when IS_VISUALIZER_CLIENT is false or missing")
  void shouldNotDelayWhenNotVisualizer() {
    DemoDelayHelper helper = new DemoDelayHelper(true);
    
    long startTime = System.currentTimeMillis();
    helper.applyDelay(Map.of(MetadataKeys.DEMO_DELAY_MS, "500"));
    long endTime = System.currentTimeMillis();
    
    assertThat(endTime - startTime).isLessThan(100);
  }

  @Test
  @DisplayName("Should apply delay when all conditions are met")
  void shouldApplyDelay() {
    DemoDelayHelper helper = new DemoDelayHelper(true);
    
    long delayMs = 100;
    long startTime = System.currentTimeMillis();
    helper.applyDelay(Map.of(
        MetadataKeys.DEMO_DELAY_MS, String.valueOf(delayMs),
        MetadataKeys.IS_VISUALIZER_CLIENT, "true"
    ));
    long endTime = System.currentTimeMillis();
    
    assertThat(endTime - startTime).isGreaterThanOrEqualTo(delayMs);
  }

  @Test
  @DisplayName("Should handle NumberFormatException gracefully when delay value is malformed")
  void shouldHandleNumberFormatException() {
    DemoDelayHelper helper = new DemoDelayHelper(true);
    
    long startTime = System.currentTimeMillis();
    helper.applyDelay(Map.of(
        MetadataKeys.DEMO_DELAY_MS, "not-a-number",
        MetadataKeys.IS_VISUALIZER_CLIENT, "true"
    ));
    long endTime = System.currentTimeMillis();
    
    // Should return immediately without throwing an exception
    assertThat(endTime - startTime).isLessThan(100);
  }

  @Test
  @DisplayName("Should handle InterruptedException and restore interrupt flag")
  void shouldHandleInterruptedException() {
    DemoDelayHelper helper = new DemoDelayHelper(true);
    
    // Interrupt the thread before calling the method
    Thread.currentThread().interrupt();
    
    helper.applyDelay(Map.of(
        MetadataKeys.DEMO_DELAY_MS, "500",
        MetadataKeys.IS_VISUALIZER_CLIENT, "true"
    ));
    
    // The method should catch InterruptedException and restore the interrupt flag
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    
    // Clear the interrupt flag for subsequent tests running on the same thread
    Thread.interrupted();
  }
}
