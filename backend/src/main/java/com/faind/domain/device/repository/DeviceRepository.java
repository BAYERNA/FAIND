package com.faind.domain.device.repository;

import com.faind.domain.device.entity.Device;
import com.faind.domain.device.entity.DeviceType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeviceRepository extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

  boolean existsBySerialNo(String serialNo);

  List<Device> findByDeviceType(DeviceType deviceType);

  List<Device> findByDeviceTypeAndStatus(DeviceType deviceType, String status);

  long countByStatusIn(List<String> statuses);
}
