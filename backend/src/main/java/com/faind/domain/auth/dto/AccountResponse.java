package com.faind.domain.auth.dto;

import com.faind.domain.auth.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

// ADM-003 annot: "수정" 진입 시 이 DTO로 전체 필드를 프리필한다 (QA 최우선 재검증 대상 수정).
public record AccountResponse(
    UUID userId,
    String name,
    String role,
    String badgeNumber,
    String team,
    String phone,
    String status,
    LocalDateTime updatedAt) {

  public static AccountResponse from(User user) {
    return new AccountResponse(
        user.getUserId(),
        user.getName(),
        user.getRole(),
        user.getBadgeNumber(),
        user.getTeam(),
        user.getPhone(),
        user.getStatus(),
        user.getUpdatedAt());
  }
}
