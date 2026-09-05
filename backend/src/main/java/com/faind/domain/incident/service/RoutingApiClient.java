package com.faind.domain.incident.service;

import com.faind.domain.incident.dto.RouteEstimateResponse;
import com.faind.global.util.GeoUtil;
import com.faind.global.util.JsonMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// FR-20(후발대 경로·ETA)와 FR-25(드론 자동출동 경로계산)가 공유하는 라우팅 계산기.
// 코드구조설계서 §2 주석대로 "FR-20/25 공용 라우팅 API 연동"을 한 곳에 모아둔다.
// 데모 규모에서는 실제 지도 라우팅 API 대신 하버사인 거리 + 평균 이동속도로 근사하며,
// 실서비스 단계에서 외부 라우팅 API 클라이언트로 교체 가능하도록 인터페이스를 좁게 유지한다.
@Component
public class RoutingApiClient {

  private static final Duration ROUTE_CACHE_TTL = Duration.ofMinutes(10);

  private final StringRedisTemplate redisTemplate;
  private final double groundVehicleAvgSpeedKmh;
  private final double droneAvgSpeedKmh;

  public RoutingApiClient(
      StringRedisTemplate redisTemplate,
      @Value("${faind.routing.ground-vehicle-avg-speed-kmh:40}") double groundVehicleAvgSpeedKmh,
      @Value("${faind.routing.drone-avg-speed-kmh:70}") double droneAvgSpeedKmh) {
    this.redisTemplate = redisTemplate;
    this.groundVehicleAvgSpeedKmh = groundVehicleAvgSpeedKmh;
    this.droneAvgSpeedKmh = droneAvgSpeedKmh;
  }

  // FR-20: 후발대(소방차) 경로 — 배정 확정 시 1회 계산.
  public RouteEstimateResponse estimateGroundRoute(
      String incidentId, BigDecimal originLat, BigDecimal originLng, BigDecimal destLat, BigDecimal destLng, String originLabel) {
    RouteEstimateResponse estimate = estimate(originLat, originLng, destLat, destLng, groundVehicleAvgSpeedKmh, originLabel);
    cache("route:" + incidentId, estimate);
    return estimate;
  }

  // FR-25: 드론 스테이션 → 현장 경로.
  public RouteEstimateResponse estimateDroneRoute(
      String incidentId, BigDecimal originLat, BigDecimal originLng, BigDecimal destLat, BigDecimal destLng, String originLabel) {
    RouteEstimateResponse estimate = estimate(originLat, originLng, destLat, destLng, droneAvgSpeedKmh, originLabel);
    cache("route:drone:" + incidentId, estimate);
    return estimate;
  }

  private RouteEstimateResponse estimate(
      BigDecimal originLat, BigDecimal originLng, BigDecimal destLat, BigDecimal destLng, double avgSpeedKmh, String originLabel) {
    if (originLat == null || originLng == null || destLat == null || destLng == null) {
      return new RouteEstimateResponse(null, -1, originLabel);
    }
    double distanceKm = GeoUtil.haversineKm(
        originLat.doubleValue(), originLng.doubleValue(), destLat.doubleValue(), destLng.doubleValue());
    int etaSeconds = (int) Math.round((distanceKm / avgSpeedKmh) * 3600);
    BigDecimal roundedDistance = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);
    return new RouteEstimateResponse(roundedDistance, etaSeconds, originLabel);
  }

  private void cache(String key, RouteEstimateResponse estimate) {
    Map<String, Object> payload = Map.of(
        "distanceKm", estimate.distanceKm() == null ? "" : estimate.distanceKm().toString(),
        "etaSeconds", estimate.etaSeconds(),
        "originLabel", estimate.originLabel() == null ? "" : estimate.originLabel());
    redisTemplate.opsForValue().set(key, JsonMapper.toJson(payload), ROUTE_CACHE_TTL);
  }
}
