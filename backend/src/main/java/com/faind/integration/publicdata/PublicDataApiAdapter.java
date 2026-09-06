package com.faind.integration.publicdata;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// 소방청 공공데이터포털 API 연동. FR-02 사전분석의 주된 조회는 ai-server(pre_analysis_agent.py)가
// 직접 수행하지만(코드구조설계서 §3), Java 쪽에서도 건물/화재이력 정보를 보조적으로 조회해야 하는
// 경우(예: CMD-001 화면 재조회, 캐시 미스 시 폴백)를 위해 동일 계약의 어댑터를 여기 둔다.
//
// 외부 공공 API가 응답 없이 연결만 물고 있을 경우를 대비해 연결 2초/응답 3초 타임아웃을 명시한다
// (AiAnalysisHttpAdapter와 동일한 이유 — 무한 대기 방지).
@Component
public class PublicDataApiAdapter {

  private final RestClient restClient;
  private final String serviceKey;

  public PublicDataApiAdapter(RestClient.Builder restClientBuilder, Environment env) {
    String baseUrl = env.getProperty("faind.integration.public-data.base-url", "https://apis.data.go.kr");
    this.serviceKey = env.getProperty("faind.integration.public-data.service-key", "");
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(3));
    this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  public Map<String, Object> fetchBuildingHazardInfo(String address) {
    if (serviceKey.isBlank()) {
      return Map.of();
    }
    return restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/1613000/BldRgstService_v2/getBrTitleInfo")
            .queryParam("serviceKey", serviceKey)
            .queryParam("address", address)
            .build())
        .retrieve()
        .body(Map.class);
  }
}
