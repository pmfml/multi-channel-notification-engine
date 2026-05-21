package com.pmfml.mcne.dtos;

import java.util.UUID;

public record NotificationEvent(UUID logId, NotificationRequest request) {
}