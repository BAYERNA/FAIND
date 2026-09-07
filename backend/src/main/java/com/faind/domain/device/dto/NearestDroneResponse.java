package com.faind.domain.device.dto;

import java.math.BigDecimal;
import java.util.UUID;

// FR-25: incident 패키지가 드론 자동배정 시 device 패키지에게 "가장 가까운 대기중 드론"을 물어볼 때 쓰는 응답.
public record NearestDroneResponse(UUID droneId, BigDecimal latitude, BigDecimal longitude, String stationLabel) {}
