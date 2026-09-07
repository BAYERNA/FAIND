package com.faind.domain.incident.dto;

import java.math.BigDecimal;

// FR-20/25 공용 라우팅 결과. DB설계서 §3.3: DB에 영속화하지 않고 Redis에 route:{incident_id}로 캐시.
public record RouteEstimateResponse(BigDecimal distanceKm, int etaSeconds, String originLabel) {}
