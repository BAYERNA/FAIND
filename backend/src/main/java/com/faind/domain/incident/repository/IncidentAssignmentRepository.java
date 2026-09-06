package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.IncidentAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentAssignmentRepository extends JpaRepository<IncidentAssignment, UUID> {

  List<IncidentAssignment> findByIncidentIdOrderByAssignedAtAsc(UUID incidentId);

  boolean existsByIncidentId(UUID incidentId);

  long countByIncidentId(UUID incidentId);

  Optional<IncidentAssignment> findByIncidentIdAndCommsLeadTrue(UUID incidentId);

  Optional<IncidentAssignment> findByIncidentIdAndUserId(UUID incidentId, UUID userId);

  // USR-001 진입 화면: 이 대원이 배정된 모든 출동(종료 포함)을 최신순으로 — 서비스 계층에서 CLOSED를 거른다.
  List<IncidentAssignment> findByUserIdOrderByAssignedAtDesc(UUID userId);
}
