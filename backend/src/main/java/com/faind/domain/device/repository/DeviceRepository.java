package com.faind.domain.device.repository;

import com.faind.domain.device.entity.Device;
import com.faind.domain.device.entity.DeviceType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

  boolean existsBySerialNo(String serialNo);

  List<Device> findByDeviceType(DeviceType deviceType);

  List<Device> findByDeviceTypeAndStatus(DeviceType deviceType, String status);

  long countByStatusIn(List<String> statuses);

  // FR-25 드론 자동배정 — status='NORMAL'을 조건절에 넣어 UPDATE 자체를 원자적인 "선점"으로 쓴다.
  // findNearestAvailableDrone()으로 후보를 고른 뒤 이걸로 확정하는 2단계라, 그 사이 다른 요청이
  // 같은 드론을 먼저 선점했으면(동시 배차 경합) 0행이 갱신되어 반환값 0으로 알 수 있다 —
  // 별도 분산 락(Redis 등) 없이 DB 트랜잭션만으로 경합을 막는다(단일 인스턴스 데모 규모에 맞는 수준).
  @Modifying
  @Query("UPDATE Device d SET d.status = 'DISPATCHED', d.updatedAt = CURRENT_TIMESTAMP "
      + "WHERE d.deviceId = :deviceId AND d.status = 'NORMAL'")
  int claimForDispatch(@Param("deviceId") UUID deviceId);
}
