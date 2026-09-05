package com.faind.domain.incident.service;

import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

// 예: 2026-0142. 데모 규모에서는 연도별 인메모리 시퀀스로 충분하다 (동시성이 커지면 DB 시퀀스로 대체).
@Component
class IncidentNumberGenerator {

  private final AtomicInteger sequence = new AtomicInteger(0);

  String next() {
    int year = Year.now().getValue();
    int seq = sequence.incrementAndGet();
    return "%d-%04d".formatted(year, seq);
  }
}
