package com.faind.domain.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// FR-01 이후 신고접수 흐름 (사람이 직접 119 신고). source=MANUAL_REPORT로 즉시 DISPATCHED 생성.
public record IncidentCreateRequest(
    @NotBlank String incidentType,
    @NotBlank String address,
    BigDecimal latitude,
    BigDecimal longitude,
    LocalDateTime reportedAt,
    @NotNull UUID commanderId) {}
