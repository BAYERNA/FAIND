package com.faind.domain.statistics.dto;

import java.util.List;

// ADM-009 기관 통계 대시보드 (FR-13, FR-27) 화면 하나를 그리기 위한 집계 응답.
// Phase 6: CCTV 자동 화재감지(FR-24) 관련 지표 3종 추가. cctvDetectionCount=0이면 아직 감지
// 이력이 없다는 뜻이라 manualDetectionRatioPercent/averageCctvDangerScore는 null로 둔다(정보 없음).
public record StatisticsSummaryResponse(
    long totalAiJudgments,
    double sopMatchAccuracyPercent,
    double reviewCompletionRatePercent,
    double averageJudgmentSeconds,
    GoldenTimeStatsResponse goldenTime,
    List<LabeledCount> monthlyJudgmentCounts,
    List<LabeledCount> judgmentTypeFrequency,
    List<RecentJudgmentItem> recentJudgments,
    long cctvDetectionCount,
    Double manualDetectionRatioPercent,
    Double averageCctvDangerScore) {}
