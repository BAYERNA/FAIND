package com.faind.integration.ai.dto;

import java.util.Map;

public record SopMatchResultDto(Map<String, Object> sopMatchResult, String riskPattern, String recommendation) {

  public static SopMatchResultDto empty() {
    return new SopMatchResultDto(Map.of(), null, null);
  }
}
