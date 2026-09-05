package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.AiJudgmentLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 쓰기 소유자는 incident 패키지. statistics 패키지의 AiJudgmentLogQueryRepository는
// 같은 테이블을 읽기 전용으로만 재사용한다 (§1 원칙 5, code구조설계서 §2 read model 주석 참조).
public interface AiJudgmentLogRepository extends JpaRepository<AiJudgmentLog, UUID> {

  // ADM-001 "AI 의심감지 대기열": related_incident_id가 아직 없는(=관제 미확인) CCTV_DETECTION row.
  List<AiJudgmentLog> findByJudgmentTypeAndRelatedIncidentIdIsNullOrderByCreatedAtDesc(String judgmentType);

  List<AiJudgmentLog> findByRelatedIncidentIdOrderByCreatedAtDesc(UUID incidentId);

  List<AiJudgmentLog> findByCreatedAtAfter(LocalDateTime after);
}
