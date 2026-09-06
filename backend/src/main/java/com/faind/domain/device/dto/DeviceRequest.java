package com.faind.domain.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

// ADM-006 신규 기기 등록. 웨어러블·스마트폰류는 currentUserId를, CCTV·드론은 latitude/longitude(+
// 선택적으로 streamUrl, 없으면 등록만 해두고 나중에 PATCH /stream-url로 채울 수 있음)를 채운다.
public record DeviceRequest(
    @NotNull String deviceType,
    @NotBlank String serialNo,
    String connectionType,
    UUID currentUserId,
    BigDecimal latitude,
    BigDecimal longitude,
    Integer batteryLevel,
    String streamUrl) {}
