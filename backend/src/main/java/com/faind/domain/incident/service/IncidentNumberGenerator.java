package com.faind.domain.incident.service;

import java.time.Year;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 예: 2026-0142. DB 시퀀스(incident_number_seq, V2 마이그레이션)를 사용한다 — 인메모리 카운터는
// 애플리케이션 재기동 시 0으로 리셋되어 기존 incidents.incident_number(UNIQUE)와 충돌하는 결함이 있었다.
@Component
class IncidentNumberGenerator {

  private final JdbcTemplate jdbcTemplate;

  IncidentNumberGenerator(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  String next() {
    int year = Year.now().getValue();
    Long seq = jdbcTemplate.queryForObject("SELECT nextval('incident_number_seq')", Long.class);
    return "%d-%04d".formatted(year, seq);
  }
}
