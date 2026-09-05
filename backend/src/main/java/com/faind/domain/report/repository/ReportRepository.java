package com.faind.domain.report.repository;

import com.faind.domain.report.entity.Report;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, UUID> {

  List<Report> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

  List<Report> findByIncidentId(UUID incidentId);
}
