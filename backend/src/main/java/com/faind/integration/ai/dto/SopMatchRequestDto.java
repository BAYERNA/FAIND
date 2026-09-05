package com.faind.integration.ai.dto;

import java.util.UUID;

// FR-08: report 패키지가 SopMatchClient를 통해 이 DTO로 ai-server의 sop_match_router를 호출한다.
public record SopMatchRequestDto(UUID reportId, String reportContent, UUID incidentId) {}
