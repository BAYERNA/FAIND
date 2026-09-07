package com.faind.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetInitialPasswordRequest(
    @NotBlank
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "8자 이상, 영문+숫자+특수문자를 포함해야 합니다.")
        String newPassword,
    @NotBlank String newPasswordConfirm) {

  public boolean isConfirmed() {
    return newPassword.equals(newPasswordConfirm);
  }
}
