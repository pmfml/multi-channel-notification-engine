package com.pmfml.mcne.dtos;

import com.pmfml.mcne.enums.NotificationChannel;
import java.util.Map;

public record NotificationRequest(
    String recipient,
    String message,
    NotificationChannel channel,
    Map<String, String> metadata

) {
}
