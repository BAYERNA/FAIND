package com.faind.domain.device.dto;

import com.faind.domain.device.entity.Device;
import java.util.UUID;

// CMD-002 라이브 카메라 선택 드롭다운 전용 — DeviceResponse보다 좁은 정보만 노출한다.
// COMMANDER가 접근하는 화면이라, 개인 대원 매핑 정보(mappedUserName 등)는 포함하지 않는다.
public record CameraResponse(UUID deviceId, String deviceType, String serialNo, String streamUrl, String status) {

  public static CameraResponse from(Device device) {
    return new CameraResponse(
        device.getDeviceId(),
        device.getDeviceType().name(),
        device.getSerialNo(),
        device.getStreamUrl(),
        device.getStatus());
  }
}
