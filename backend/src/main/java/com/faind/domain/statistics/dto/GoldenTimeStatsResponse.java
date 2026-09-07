package com.faind.domain.statistics.dto;

// FR-27: "기존 평균 출동시간 vs 드론 활용 시 예상 단축시간" 비교 카드.
public record GoldenTimeStatsResponse(int existingAverageSeconds, Integer droneAverageArrivalSeconds, Integer reductionSeconds) {

  public static GoldenTimeStatsResponse of(int existingAverageSeconds, Double droneAverageArrivalSeconds) {
    if (droneAverageArrivalSeconds == null) {
      return new GoldenTimeStatsResponse(existingAverageSeconds, null, null);
    }
    int droneSeconds = (int) Math.round(droneAverageArrivalSeconds);
    return new GoldenTimeStatsResponse(existingAverageSeconds, droneSeconds, Math.max(existingAverageSeconds - droneSeconds, 0));
  }
}
