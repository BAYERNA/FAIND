package com.faind.domain.incident.repository;

import com.faind.domain.incident.entity.ResponderStatusLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponderStatusLogRepository extends JpaRepository<ResponderStatusLog, UUID> {

  // CMD-002 annot#1: risk_level 기준 자동정렬을 위해 출동 건의 로그를 최신순으로 모두 가져온 뒤
  // 서비스 레이어에서 user_id별 최신 1건만 남겨 정렬한다 (로그 테이블 특성상 최신값이 여러 건 존재).
  List<ResponderStatusLog> findByIncidentIdOrderByRecordedAtDesc(UUID incidentId);

  List<ResponderStatusLog> findByIncidentIdAndUserIdOrderByRecordedAtDesc(UUID incidentId, UUID userId);
}
