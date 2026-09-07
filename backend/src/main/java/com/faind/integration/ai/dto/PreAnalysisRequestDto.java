package com.faind.integration.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;

// FR-02: Python AI서버(ai-server)의 pre_analysis_router가 기대하는 요청 페이로드.
public record PreAnalysisRequestDto(UUID incidentId, String address, BigDecimal latitude, BigDecimal longitude) {}
