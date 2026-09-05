package com.faind.domain.incident.dto;

import com.faind.domain.incident.entity.ResponderStatusLog;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// CMD-002 annot#1: risk_level 기준(위험→주의→정상) 자동 정렬된 목록의 각 행.
public record ResponderStatusResponse(
    UUID userId,
    Map<String, Object> biometricData,
    Map<String, Object> environmentData,
    String riskLevel,
    String connectionStatus,
    LocalDateTime recordedAt) {

  public static ResponderStatusResponse from(ResponderStatusLog log) {
    return new ResponderStatusResponse(
        log.getUserId(),
        log.getBiometricData(),
        log.getEnvironmentData(),
        log.getRiskLevel(),
        log.getConnectionStatus(),
        log.getRecordedAt());
  }
}
