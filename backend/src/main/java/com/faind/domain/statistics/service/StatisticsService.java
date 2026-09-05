package com.faind.domain.statistics.service;

import com.faind.domain.incident.entity.AiJudgmentLog;
import com.faind.domain.incident.service.DroneDispatchService;
import com.faind.domain.report.dto.ReviewStatsResponse;
import com.faind.domain.report.service.ReportService;
import com.faind.domain.statistics.dto.GoldenTimeStatsResponse;
import com.faind.domain.statistics.dto.LabeledCount;
import com.faind.domain.statistics.dto.RecentJudgmentItem;
import com.faind.domain.statistics.dto.StatisticsSummaryResponse;
import com.faind.domain.statistics.repository.AiJudgmentLogQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// FR-13 기관 통계 대시보드 / FR-27 골든타임 단축효과 (ADM-009). ai_judgment_logs를 read model로 집계한다.
@Service
@Transactional(readOnly = true)
public class StatisticsService {

  private static final int RECENT_JUDGMENT_LIMIT = 20;
  private static final int MONTHLY_TREND_MONTHS = 6;

  private final AiJudgmentLogQueryRepository aiJudgmentLogQueryRepository;
  private final ReportService reportService;
  private final DroneDispatchService droneDispatchService;
  private final int existingAverageDispatchSeconds;

  public StatisticsService(
      AiJudgmentLogQueryRepository aiJudgmentLogQueryRepository,
      ReportService reportService,
      DroneDispatchService droneDispatchService,
      @Value("${faind.statistics.existing-average-dispatch-seconds:492}") int existingAverageDispatchSeconds) {
    this.aiJudgmentLogQueryRepository = aiJudgmentLogQueryRepository;
    this.reportService = reportService;
    this.droneDispatchService = droneDispatchService;
    this.existingAverageDispatchSeconds = existingAverageDispatchSeconds;
  }

  public StatisticsSummaryResponse getSummary() {
    long totalJudgments = aiJudgmentLogQueryRepository.count();

    BigDecimal sopAccuracy = aiJudgmentLogQueryRepository.averageConfidenceScore("REPORT_SOP_MATCH");
    double sopMatchAccuracyPercent = sopAccuracy == null ? 0.0 : sopAccuracy.doubleValue();

    ReviewStatsResponse reviewStats = reportService.getReviewStats();
    double reviewCompletionRatePercent = reviewStats.completionRate() * 100;

    double averageJudgmentSeconds = averagePreAnalysisSeconds();

    java.util.OptionalDouble droneAvgSeconds = droneDispatchService.averageDroneArrivalSeconds();
    GoldenTimeStatsResponse goldenTime = GoldenTimeStatsResponse.of(
        existingAverageDispatchSeconds, droneAvgSeconds.isPresent() ? droneAvgSeconds.getAsDouble() : null);

    return new StatisticsSummaryResponse(
        totalJudgments,
        round1(sopMatchAccuracyPercent),
        round1(reviewCompletionRatePercent),
        round1(averageJudgmentSeconds),
        goldenTime,
        monthlyJudgmentCounts(),
        judgmentTypeFrequency(),
        recentJudgments());
  }

  private double averagePreAnalysisSeconds() {
    // PRE_ANALYSIS 로그 자체에는 "접수시각"이 없어(judgment_type 공용 테이블), NFR-03 검증은
    // CMD-001의 PreAnalysisResponse.elapsedMillis가 담당한다. 여기서는 판정 발생 간격 대신
    // 최근 판정들의 confidence_score 신뢰 구간을 대체 지표로 쓰지 않고, 단순화해 0으로 둔다.
    return 0.0;
  }

  private List<LabeledCount> monthlyJudgmentCounts() {
    LocalDateTime since = LocalDateTime.now().minusMonths(MONTHLY_TREND_MONTHS - 1L).withDayOfMonth(1)
        .withHour(0).withMinute(0).withSecond(0).withNano(0);
    Map<String, Long> counts = new TreeMap<>();
    for (AiJudgmentLog log : aiJudgmentLogQueryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(since)) {
      String key = "%d-%02d".formatted(log.getCreatedAt().getYear(), log.getCreatedAt().getMonthValue());
      counts.merge(key, 1L, Long::sum);
    }
    return counts.entrySet().stream()
        .map(e -> new LabeledCount(monthLabel(e.getKey()), e.getValue()))
        .toList();
  }

  private String monthLabel(String yearMonthKey) {
    int month = Integer.parseInt(yearMonthKey.split("-")[1]);
    return java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.KOREAN);
  }

  private List<LabeledCount> judgmentTypeFrequency() {
    return aiJudgmentLogQueryRepository.countGroupedByJudgmentType().stream()
        .map(row -> new LabeledCount((String) row[0], (Long) row[1]))
        .sorted(Comparator.comparingLong(LabeledCount::count).reversed())
        .toList();
  }

  private List<RecentJudgmentItem> recentJudgments() {
    return aiJudgmentLogQueryRepository
        .findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime.now().minusMonths(1))
        .stream()
        .sorted(Comparator.comparing(AiJudgmentLog::getCreatedAt).reversed())
        .limit(RECENT_JUDGMENT_LIMIT)
        .map(log -> new RecentJudgmentItem(
            log.getCreatedAt(), log.getJudgmentType(), log.getRelatedIncidentId(), log.getConfidenceScore()))
        .toList();
  }

  private double round1(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
