package com.faind.global.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
        .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
        .toList();
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus()).body(ErrorResponse.of(ErrorCode.FORBIDDEN));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
    log.warn("Data integrity violation", e);
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "데이터 제약조건을 위반했습니다."));
  }

  // Device.remap/relocate/updateStreamUrl 등 도메인 상태 위반(IllegalStateException)을
  // 500(INTERNAL_ERROR)이 아니라 400으로 응답한다 — 클라이언트 입력(기기 유형 불일치 등)이
  // 원인이므로 서버 오류로 취급하지 않는다.
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, e.getMessage()));
  }

  // 요청 본문이 JSON 문법에 안 맞거나(깨진 JSON) 필드 타입이 안 맞는 경우(예: UUID 자리에
  // "COMMANDER_UUID" 같은 플레이스홀더 문자열) — 서버 버그가 아니라 클라이언트 입력 문제이므로
  // 500(INTERNAL_ERROR) 대신 400으로 응답하고, 에러 로그도 오탐(false alarm)으로 남기지 않는다.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 본문 형식이 올바르지 않습니다."));
  }

  // 쿼리/경로 파라미터 타입이 안 맞는 경우(예: 숫자 자리에 문자열) — 위와 같은 이유로 400 처리.
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "'" + e.getName() + "' 파라미터 형식이 올바르지 않습니다."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
    log.error("Unexpected error", e);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
  }
}
