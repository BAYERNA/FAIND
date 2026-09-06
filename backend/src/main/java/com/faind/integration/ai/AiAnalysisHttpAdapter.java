package com.faind.integration.ai;

import com.faind.integration.ai.dto.PreAnalysisRequestDto;
import com.faind.integration.ai.dto.PreAnalysisResultDto;
import com.faind.integration.ai.dto.SopMatchRequestDto;
import com.faind.integration.ai.dto.SopMatchResultDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// ai-server(Python/FastAPI)를 REST로 호출하는 실제 구현체. NFR-03(3초 이내)을 지키기 위해
// 커넥션/응답 타임아웃을 짧게 잡고, 실패 시에는 예외로 전파하지 않고 빈 결과로 대체(fallback)한다 —
// AI 분석이 실패해도 신고접수·출동 자체(FR-01/02 핵심 경로)는 막히면 안 되기 때문 (NFR-07 보조적 지위와도 일치).
//
// 타임아웃을 명시하지 않으면 RestClient 기본 HttpClient는 무한 대기한다 — ai-server가 응답 없이
// 연결만 물고 있을 때 CircuitBreaker의 "실패 감지"보다 먼저 요청 스레드가 영원히 막혀버려
// NFR-03을 정면으로 위반하는 결함이 있었다. 연결 2초/응답 3초로 명시해 이를 막는다.
//
// HTTP 버전도 1.1로 고정한다 — JDK HttpClient는 기본이 HTTP_2(ALPN 실패 시 자동 폴백)인데,
// cleartext(h2c) 연결에서 업그레이드 헤더를 보내는 방식이 uvicorn의 엄격한 HTTP 파서(httptools)에서
// "Invalid HTTP request received." 400으로 거부되는 결함이 있었다.
@Component
public class AiAnalysisHttpAdapter implements AiAnalysisPort {

  private static final Logger log = LoggerFactory.getLogger(AiAnalysisHttpAdapter.class);

  private final RestClient restClient;

  public AiAnalysisHttpAdapter(RestClient.Builder restClientBuilder, org.springframework.core.env.Environment env) {
    String baseUrl = env.getProperty("faind.integration.ai-server.base-url", "http://localhost:8001");
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(3));
    this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  @Override
  @CircuitBreaker(name = "aiServer", fallbackMethod = "preAnalysisFallback")
  public PreAnalysisResultDto requestPreAnalysis(PreAnalysisRequestDto request) {
    return restClient.post()
        .uri("/api/v1/pre-analysis")
        .body(request)
        .retrieve()
        .body(PreAnalysisResultDto.class);
  }

  @Override
  @CircuitBreaker(name = "aiServer", fallbackMethod = "sopMatchFallback")
  public SopMatchResultDto requestSopMatch(SopMatchRequestDto request) {
    return restClient.post()
        .uri("/api/v1/sop-match")
        .body(request)
        .retrieve()
        .body(SopMatchResultDto.class);
  }

  @SuppressWarnings("unused")
  private PreAnalysisResultDto preAnalysisFallback(PreAnalysisRequestDto request, Throwable throwable) {
    log.warn("ai-server 사전분석 호출 실패 — 빈 결과로 대체 (incidentId={})", request.incidentId(), throwable);
    return PreAnalysisResultDto.empty();
  }

  @SuppressWarnings("unused")
  private SopMatchResultDto sopMatchFallback(SopMatchRequestDto request, Throwable throwable) {
    log.warn("ai-server SOP대조 호출 실패 — 빈 결과로 대체 (reportId={})", request.reportId(), throwable);
    return SopMatchResultDto.empty();
  }
}
