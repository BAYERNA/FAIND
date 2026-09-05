package com.faind.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

// CMN-001: 기관선택은 데모 스코프에서 단일 기관 고정이므로 클라이언트가 별도로 넘기지 않는다.
public record LoginRequest(@NotBlank String badgeNumber, @NotBlank String password) {}
