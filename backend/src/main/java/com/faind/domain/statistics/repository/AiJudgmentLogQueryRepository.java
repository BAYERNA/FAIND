package com.faind.domain.statistics.repository;

import com.faind.domain.incident.entity.AiJudgmentLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

// 코드구조설계서 §2 statistics 패키지 주석: "ai_judgment_logs 읽기 전용". incident 패키지가 쓰기를
// 소유하는 같은 테이블(ai_judgment_logs)을, DB설계서 §1 원칙 5(재사용 우선)에 따라 read model로
// 재사용한다 — 그래서 CRUD가 아닌 Spring Data 기본 Repository<T,ID>만 상속해 조회 메서드만 연다.
public interface AiJudgmentLogQueryRepository extends Repository<AiJudgmentLog, UUID> {

  long countByJudgmentType(String judgmentType);

  long count();

  List<AiJudgmentLog> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime after);

  @Query("select avg(l.confidenceScore) from AiJudgmentLog l where l.judgmentType = :judgmentType")
  BigDecimal averageConfidenceScore(String judgmentType);

  @Query("select l.judgmentType, count(l) from AiJudgmentLog l group by l.judgmentType")
  List<Object[]> countGroupedByJudgmentType();

  List<AiJudgmentLog> findByJudgmentTypeAndSourceDeviceIdIsNotNullOrderByCreatedAtDesc(String judgmentType);
}
