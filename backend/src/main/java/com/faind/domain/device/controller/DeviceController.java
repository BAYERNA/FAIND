package com.faind.domain.device.controller;

import com.faind.domain.device.dto.DeviceRequest;
import com.faind.domain.device.dto.DeviceResponse;
import com.faind.domain.device.service.DeviceMappingService;
import com.faind.domain.device.service.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// ADM-006 기기 · 장비 매핑 관리 (FR-11, NFR-05, FR-24 CCTV·드론 자산 등록)
@RestController
@RequestMapping("/api/v1/devices")
@PreAuthorize("hasRole('ADMIN')")
public class DeviceController {

  private final DeviceService deviceService;
  private final DeviceMappingService deviceMappingService;

  public DeviceController(DeviceService deviceService, DeviceMappingService deviceMappingService) {
    this.deviceService = deviceService;
    this.deviceMappingService = deviceMappingService;
  }

  @GetMapping
  public ResponseEntity<Page<DeviceResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "ALL") String deviceType,
      Pageable pageable) {
    return ResponseEntity.ok(deviceService.list(keyword, deviceType, pageable));
  }

  // CMD-002 드론 정찰 카드: 지휘관도 배정된 드론 기기 상세(배터리·상태)는 조회 가능해야 한다.
  @GetMapping("/{deviceId}")
  @PreAuthorize("hasAnyRole('COMMANDER','ADMIN')")
  public ResponseEntity<DeviceResponse> get(@PathVariable UUID deviceId) {
    return ResponseEntity.ok(deviceService.get(deviceId));
  }

  @PostMapping
  public ResponseEntity<DeviceResponse> register(@Valid @RequestBody DeviceRequest request) {
    return ResponseEntity.ok(deviceService.register(request));
  }

  @PatchMapping("/{deviceId}/mapping")
  public ResponseEntity<DeviceResponse> remap(@PathVariable UUID deviceId, @RequestBody RemapRequest request) {
    return ResponseEntity.ok(deviceMappingService.remap(deviceId, request.userId()));
  }

  @PatchMapping("/{deviceId}/location")
  public ResponseEntity<DeviceResponse> relocate(@PathVariable UUID deviceId, @RequestBody RelocateRequest request) {
    return ResponseEntity.ok(deviceService.relocate(deviceId, request.latitude(), request.longitude()));
  }

  public record RemapRequest(@NotNull UUID userId) {}

  public record RelocateRequest(@NotNull BigDecimal latitude, @NotNull BigDecimal longitude) {}
}
