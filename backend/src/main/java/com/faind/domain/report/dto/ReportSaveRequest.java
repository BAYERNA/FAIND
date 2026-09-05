package com.faind.domain.report.dto;

// USR-002 보고서 작성 화면의 "임시 저장"/"제출" 공용 요청 바디.
public record ReportSaveRequest(String content, String videoRef) {}
