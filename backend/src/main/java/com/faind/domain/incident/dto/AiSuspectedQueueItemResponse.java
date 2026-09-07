package com.faind.domain.incident.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ADM-001 "AI 의심감지 대기열" 표의 한 행 (FR-24, NFR-08).
public record AiSuspectedQueueItemResponse(
    UUID incidentId,
    UUID judgmentId,
    UUID sourceDeviceId,
    String address,
    LocalDateTime detectedAt,
    BigDecimal confidenceScore,
    String status) {}
