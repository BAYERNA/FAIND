package com.faind.domain.incident.dto;

import com.faind.domain.incident.entity.DroneDispatch;
import java.time.LocalDateTime;
import java.util.UUID;

public record DroneDispatchResponse(
    UUID dispatchId, UUID droneId, LocalDateTime dispatchedAt, LocalDateTime arrivedAt, String status, String videoRef) {

  public static DroneDispatchResponse from(DroneDispatch dispatch) {
    return new DroneDispatchResponse(
        dispatch.getDispatchId(),
        dispatch.getDroneId(),
        dispatch.getDispatchedAt(),
        dispatch.getArrivedAt(),
        dispatch.getStatus(),
        dispatch.getVideoRef());
  }
}
