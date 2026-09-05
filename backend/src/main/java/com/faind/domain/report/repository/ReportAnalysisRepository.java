package com.faind.domain.report.repository;

import com.faind.domain.report.entity.ReportAnalysis;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAnalysisRepository extends JpaRepository<ReportAnalysis, UUID> {

  Optional<ReportAnalysis> findByReportId(UUID reportId);

  long countByReviewStatus(String reviewStatus);
}
