package com.faind.domain.incident.dto;

import com.faind.domain.incident.entity.AiJudgmentLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// CMD-002 드론 정찰 카드(FR-26)·CMD-001 등에서 이 출동에 얽힌 AI 판단 이력을 시간순으로 보여줄 때 사용.
public record AiJudgmentSummaryResponse(
    UUID judgmentId, String judgmentType, UUID sourceDeviceId, BigDecimal confidenceScore, String summary,
    LocalDateTime createdAt) {

  public static AiJudgmentSummaryResponse from(AiJudgmentLog log) {
    return new AiJudgmentSummaryResponse(
        log.getJudgmentId(), log.getJudgmentType(), log.getSourceDeviceId(), log.getConfidenceScore(),
        log.getSummary(), log.getCreatedAt());
  }
}
