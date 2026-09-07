package com.faind.domain.incident.dto;

import com.faind.domain.incident.entity.PreAnalysisResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// CMD-001: 헤더에 "사전분석 완료 (2.1초)"처럼 소요시간을 표시해 NFR-03(3초 이내) 검증을 가능하게 한다.
public record PreAnalysisResponse(
    UUID incidentId,
    Map<String, Object> buildingInfo,
    Map<String, Object> hazardInfo,
    Map<String, Object> fireHistoryInfo,
    String dataSource,
    long elapsedMillis) {

  public static PreAnalysisResponse from(PreAnalysisResult result, LocalDateTime reportedAt) {
    long elapsedMillis = Duration.between(reportedAt, result.getAnalyzedAt()).toMillis();
    return new PreAnalysisResponse(
        result.getIncidentId(),
        result.getBuildingInfo(),
        result.getHazardInfo(),
        result.getFireHistoryInfo(),
        result.getDataSource(),
        Math.max(elapsedMillis, 0));
  }
}
