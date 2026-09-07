package com.faind.domain.incident.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignmentRequest(@NotNull UUID userId, String roleInIncident) {}
