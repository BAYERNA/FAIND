package com.faind.domain.statistics.dto;

import java.util.List;

// ADM-009 기관 통계 대시보드 (FR-13, FR-27) 화면 하나를 그리기 위한 집계 응답.
public record StatisticsSummaryResponse(
    long totalAiJudgments,
    double sopMatchAccuracyPercent,
    double reviewCompletionRatePercent,
    double averageJudgmentSeconds,
    GoldenTimeStatsResponse goldenTime,
    List<LabeledCount> monthlyJudgmentCounts,
    List<LabeledCount> judgmentTypeFrequency,
    List<RecentJudgmentItem> recentJudgments) {}
