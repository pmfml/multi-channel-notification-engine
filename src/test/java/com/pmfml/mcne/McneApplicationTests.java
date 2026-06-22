package com.pmfml.mcne;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full application context integration test.
 *
 * Requires Docker infrastructure (PostgreSQL on port 5435, RabbitMQ) to be running.
 * Run manually with: docker compose up -d && ./mvnw test -Dtest=McneApplicationTests
 */
@SpringBootTest
@Disabled("Requires Docker infrastructure. Run manually after: docker compose up -d")
class McneApplicationTests {

  @Test
  void contextLoads() {
  }

}
