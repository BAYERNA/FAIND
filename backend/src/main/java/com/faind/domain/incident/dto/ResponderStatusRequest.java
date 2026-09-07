package com.faind.domain.incident.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.UUID;

public record ResponderStatusRequest(
    @NotNull UUID userId,
    Map<String, Object> biometricData,
    Map<String, Object> environmentData,
    @Pattern(regexp = "NORMAL|CAUTION|DANGER") String riskLevel,
    @Pattern(regexp = "CONNECTED|MESH|SMS|DISCONNECTED") String connectionStatus) {}
