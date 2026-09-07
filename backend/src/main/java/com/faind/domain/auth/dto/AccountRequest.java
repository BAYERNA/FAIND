package com.faind.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// ADM-003 계정 등록·수정 공용 요청. 등록 시에는 badgeNumber 필수, 수정 시에는 무시된다(사번은 불변 식별자).
public record AccountRequest(
    @NotBlank String name,
    @Pattern(regexp = "ADMIN|COMMANDER|RESPONDER", message = "역할은 ADMIN/COMMANDER/RESPONDER 중 하나여야 합니다.")
        String role,
    String badgeNumber,
    String team,
    String phone) {}
