package com.faind.domain.incident.dto;

// ADM-001 관리자 홈 (FR-09) 4개 통계 카드: 금일 출동 / 진행중 출동 / 근무 대원 / 기기 이상
public record DashboardSummaryResponse(
    long todayDispatchCount, long inProgressCount, long onDutyResponderCount, long deviceAnomalyCount) {}
