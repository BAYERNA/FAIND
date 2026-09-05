package com.faind.domain.report.dto;

// statistics 패키지가 FR-13 "검토 완료율"을 계산할 때 report 패키지 인터페이스를 통해 받는 집계값.
public record ReviewStatsResponse(long totalAnalyses, long reviewedAnalyses) {

  public double completionRate() {
    return totalAnalyses == 0 ? 0.0 : (double) reviewedAnalyses / totalAnalyses;
  }
}
