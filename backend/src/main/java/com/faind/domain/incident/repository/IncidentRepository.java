package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.Incident;
import com.faind.domain.incident.entity.IncidentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

  Optional<Incident> findByIncidentNumber(String incidentNumber);

  List<Incident> findByStatusOrderByReportedAtDesc(IncidentStatus status);

  List<Incident> findByStatusInOrderByReportedAtDesc(List<IncidentStatus> statuses);

  Page<Incident> findAllByOrderByReportedAtDesc(Pageable pageable);

  long countByStatus(IncidentStatus status);

  long countByReportedAtAfter(java.time.LocalDateTime after);
}
