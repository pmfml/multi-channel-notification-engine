package com.pmfml.mcne.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Enable a simple memory-based message broker to carry the messages back to the client on destinations prefixed with "/topic"
    config.enableSimpleBroker("/topic");
    
    // Prefix for messages that are bound for methods annotated with @MessageMapping (if we needed the client to send messages to the server via WS)
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // The endpoint that clients will use to connect to our WebSocket server
    registry.addEndpoint("/ws-mcne")
            .setAllowedOriginPatterns("*"); // Allow all origins for the development frontend
  }
}
