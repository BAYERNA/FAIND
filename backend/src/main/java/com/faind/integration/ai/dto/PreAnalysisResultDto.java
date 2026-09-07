package com.faind.integration.ai.dto;

import java.util.Map;

public record PreAnalysisResultDto(
    Map<String, Object> buildingInfo, Map<String, Object> hazardInfo, Map<String, Object> fireHistoryInfo) {

  public static PreAnalysisResultDto empty() {
    return new PreAnalysisResultDto(Map.of(), Map.of(), Map.of());
  }
}
