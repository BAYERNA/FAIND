package com.faind.domain.device.entity;

// DB설계서 §3.2: BODYCAM/SMARTPHONE/DIGITAL_MASK/SENSOR(대원 휴대) + CCTV/DRONE(고정 위치 자산, v2.0 신규)
public enum DeviceType {
  BODYCAM,
  SMARTPHONE,
  DIGITAL_MASK,
  SENSOR,
  CCTV,
  DRONE;

  public boolean isPersonalDevice() {
    return this == BODYCAM || this == SMARTPHONE || this == DIGITAL_MASK || this == SENSOR;
  }

  public boolean isFixedLocationAsset() {
    return this == CCTV || this == DRONE;
  }
}
