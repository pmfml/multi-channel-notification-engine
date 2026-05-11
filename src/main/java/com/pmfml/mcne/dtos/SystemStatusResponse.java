package com.pmfml.mcne.dtos;

import java.time.Instant;

public record SystemStatusResponse(
    String status,
    String environment,
    Instant timestamp

) {
}
