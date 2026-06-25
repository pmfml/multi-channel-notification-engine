package com.pmfml.mcne.controllers;

import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final RabbitListenerEndpointRegistry registry;

    public ConfigController(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    /**
     * Dynamically sets the concurrency level of the main notification consumer.
     * If count is 0, the consumer is stopped (simulating a crash or pause).
     * If count > 0, the consumer is started (if stopped) and scaled to {count} threads.
     * 
     * @param count the number of concurrent consumers (0-10)
     * @return success message
     */
    @PutMapping("/concurrency")
    public ResponseEntity<Map<String, String>> setConcurrency(@RequestParam int count) {
        SimpleMessageListenerContainer container = 
            (SimpleMessageListenerContainer) registry.getListenerContainer("notificationConsumer");

        if (container != null) {
            if (count <= 0) {
                container.stop();
            } else {
                if (!container.isRunning()) {
                    container.start();
                }
                container.setConcurrentConsumers(count);
                container.setMaxConcurrentConsumers(count);
            }
            return ResponseEntity.ok(Map.of("message", "Concurrency updated to " + count));
        }
        
        return ResponseEntity.notFound().build();
    }
}
