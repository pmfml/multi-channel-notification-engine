package com.pmfml.mcne.dtos;

import com.pmfml.mcne.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import com.pmfml.mcne.validation.ValidRecipient;

@ValidRecipient
public record NotificationRequest(
    @NotBlank(message = "Recipient is required") String recipient,
    @NotBlank(message = "Message is required") String message,
    @NotNull(message = "Channel is required") NotificationChannel channel,
    Map<String, String> metadata
) {
}
