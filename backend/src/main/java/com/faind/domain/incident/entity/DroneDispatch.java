package com.faind.domain.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

// DB설계서 §3.12 drone_dispatches (신규). FR-25/26.
@Entity
@Table(name = "drone_dispatches")
public class DroneDispatch {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "dispatch_id")
  private UUID dispatchId;

  @Column(name = "incident_id", nullable = false)
  private UUID incidentId;

  @Column(name = "drone_id", nullable = false)
  private UUID droneId;

  @Column(name = "dispatched_at", nullable = false)
  private LocalDateTime dispatchedAt;

  @Column(name = "arrived_at")
  private LocalDateTime arrivedAt;

  @Column(nullable = false, length = 15)
  private String status = "EN_ROUTE"; // EN_ROUTE / ON_SITE / RETURNED

  @Column(name = "video_ref")
  private String videoRef;

  protected DroneDispatch() {}

  public DroneDispatch(UUID incidentId, UUID droneId) {
    this.incidentId = incidentId;
    this.droneId = droneId;
    this.dispatchedAt = LocalDateTime.now();
    this.status = "EN_ROUTE";
  }

  public void markOnSite(String videoRef) {
    this.arrivedAt = LocalDateTime.now();
    this.status = "ON_SITE";
    this.videoRef = videoRef;
  }

  public void markReturned() {
    this.status = "RETURNED";
  }

  public UUID getDispatchId() {
    return dispatchId;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public UUID getDroneId() {
    return droneId;
  }

  public LocalDateTime getDispatchedAt() {
    return dispatchedAt;
  }

  public LocalDateTime getArrivedAt() {
    return arrivedAt;
  }

  public String getStatus() {
    return status;
  }

  public String getVideoRef() {
    return videoRef;
  }
}
