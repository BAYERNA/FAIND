package com.faind.domain.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecentJudgmentItem(
    LocalDateTime createdAt, String judgmentType, UUID relatedIncidentId, BigDecimal confidenceScore) {}
