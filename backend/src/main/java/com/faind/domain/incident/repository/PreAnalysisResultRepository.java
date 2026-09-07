package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.PreAnalysisResult;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreAnalysisResultRepository extends JpaRepository<PreAnalysisResult, UUID> {

  Optional<PreAnalysisResult> findByIncidentId(UUID incidentId);
}
