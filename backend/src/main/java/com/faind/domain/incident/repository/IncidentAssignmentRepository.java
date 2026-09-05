package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.IncidentAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAssignmentRepository extends JpaRepository<IncidentAssignment, UUID> {

  List<IncidentAssignment> findByIncidentIdOrderByAssignedAtAsc(UUID incidentId);

  boolean existsByIncidentId(UUID incidentId);

  Optional<IncidentAssignment> findByIncidentIdAndCommsLeadTrue(UUID incidentId);

  Optional<IncidentAssignment> findByIncidentIdAndUserId(UUID incidentId, UUID userId);
}
