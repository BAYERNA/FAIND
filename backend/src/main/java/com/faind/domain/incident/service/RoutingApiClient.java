package com.faind.domain.incident.service;

import com.faind.domain.incident.dto.RouteEstimateResponse;
import com.faind.global.util.GeoUtil;
import com.faind.global.util.JsonMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
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
  private final BigDecimal fireStationLatitude;
  private final BigDecimal fireStationLongitude;
  private final String fireStationLabel;

  public RoutingApiClient(
      StringRedisTemplate redisTemplate,
      @Value("${faind.routing.ground-vehicle-avg-speed-kmh:40}") double groundVehicleAvgSpeedKmh,
      @Value("${faind.routing.drone-avg-speed-kmh:70}") double droneAvgSpeedKmh,
      @Value("${faind.routing.fire-station-latitude:37.5665}") BigDecimal fireStationLatitude,
      @Value("${faind.routing.fire-station-longitude:126.9780}") BigDecimal fireStationLongitude,
      @Value("${faind.routing.fire-station-label:OO소방서}") String fireStationLabel) {
    this.redisTemplate = redisTemplate;
    this.groundVehicleAvgSpeedKmh = groundVehicleAvgSpeedKmh;
    this.droneAvgSpeedKmh = droneAvgSpeedKmh;
    this.fireStationLatitude = fireStationLatitude;
    this.fireStationLongitude = fireStationLongitude;
    this.fireStationLabel = fireStationLabel;
  }

  // FR-20: 후발대(소방차) 경로 — 배정 확정 시 1회 계산.
  public RouteEstimateResponse estimateGroundRoute(
      String incidentId, BigDecimal originLat, BigDecimal originLng, BigDecimal destLat, BigDecimal destLng, String originLabel) {
    RouteEstimateResponse estimate = estimate(originLat, originLng, destLat, destLng, groundVehicleAvgSpeedKmh, originLabel);
    cache("route:" + incidentId, estimate);
    return estimate;
  }

  // FR-20: DB설계서에 소방서·차량 위치 테이블이 없어(드론처럼 device로 위치를 추적하지 않음),
  // 관할 소방서 고정 좌표(faind.routing.fire-station-*)를 출발지로 근사한다.
  public RouteEstimateResponse estimateGroundRouteFromStation(String incidentId, BigDecimal destLat, BigDecimal destLng) {
    return estimateGroundRoute(incidentId, fireStationLatitude, fireStationLongitude, destLat, destLng, fireStationLabel);
  }

  // CMD-001/002가 배정 시점에 캐시된 결과를 읽어 표시한다. TTL(10분) 만료 시 재계산하지 않고
  // "정보 없음"을 그대로 보여준다 — 없는 값을 임의로 만들어내지 않는다는 원칙을 여기서도 지킨다.
  public Optional<RouteEstimateResponse> getCachedGroundRoute(String incidentId) {
    return readCached("route:" + incidentId);
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

  private Optional<RouteEstimateResponse> readCached(String key) {
    String json = redisTemplate.opsForValue().get(key);
    if (json == null) {
      return Optional.empty();
    }
    Map<String, Object> payload = JsonMapper.toMap(json);
    String distanceKmRaw = (String) payload.get("distanceKm");
    BigDecimal distanceKm = distanceKmRaw == null || distanceKmRaw.isBlank() ? null : new BigDecimal(distanceKmRaw);
    int etaSeconds = ((Number) payload.get("etaSeconds")).intValue();
    String originLabel = (String) payload.get("originLabel");
    return Optional.of(new RouteEstimateResponse(distanceKm, etaSeconds, originLabel == null || originLabel.isBlank() ? null : originLabel));
  }
}
