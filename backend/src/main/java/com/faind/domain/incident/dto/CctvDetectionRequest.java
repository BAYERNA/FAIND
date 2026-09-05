package com.faind.domain.incident.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ai-server의 fire_detection_router가 CCTV 스트림에서 화재를 의심 감지했을 때 콜백으로 보내는 페이로드.
// (코드구조설계서 §3 fire_detection_router.py "감지결과 수신 + Java 모놀리식으로 콜백")
public record CctvDetectionRequest(
    @NotNull UUID cameraDeviceId,
    String addressHint,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal confidenceScore,
    String summary,
    LocalDateTime detectedAt) {}
