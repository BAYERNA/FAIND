package com.faind.domain.incident.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DroneReconRequest(@NotNull BigDecimal confidenceScore, String summary, String videoRef) {}
