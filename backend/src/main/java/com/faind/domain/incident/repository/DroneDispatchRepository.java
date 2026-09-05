package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.DroneDispatch;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroneDispatchRepository extends JpaRepository<DroneDispatch, UUID> {

  List<DroneDispatch> findByIncidentIdOrderByDispatchedAtDesc(UUID incidentId);
}
