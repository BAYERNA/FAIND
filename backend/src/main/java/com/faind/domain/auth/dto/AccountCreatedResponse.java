package com.faind.domain.auth.dto;

// ADM-003 annot: "추가 비밀번호 자동 생성" — 등록 시점에만 1회 평문으로 내려주고 서버는 해시만 보관한다.
public record AccountCreatedResponse(AccountResponse account, String issuedTemporaryPassword) {}
