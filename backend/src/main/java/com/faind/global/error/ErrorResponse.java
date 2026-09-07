package com.faind.global.error;

import java.time.Instant;
import java.util.List;

// NFR-02: 저장/제출 액션은 서버 응답(200)만으로는 성공 여부를 판단할 수 없으므로,
// 실패 시 프론트가 명시적으로 처리할 수 있도록 항상 code/message 구조를 내려준다.
public record ErrorResponse(String code, String message, Instant timestamp, List<FieldError> fieldErrors) {

  public record FieldError(String field, String reason) {}

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), Instant.now(), List.of());
  }

  public static ErrorResponse of(ErrorCode errorCode, String message) {
    return new ErrorResponse(errorCode.getCode(), message, Instant.now(), List.of());
  }

  public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
    return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), Instant.now(), fieldErrors);
  }
}
