package com.faind.integration.ai;

import com.faind.integration.ai.dto.PreAnalysisRequestDto;
import com.faind.integration.ai.dto.PreAnalysisResultDto;
import com.faind.integration.ai.dto.SopMatchRequestDto;
import com.faind.integration.ai.dto.SopMatchResultDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// ai-server(Python/FastAPI)를 REST로 호출하는 실제 구현체. NFR-03(3초 이내)을 지키기 위해
// 커넥션/응답 타임아웃을 짧게 잡고, 실패 시에는 예외로 전파하지 않고 빈 결과로 대체(fallback)한다 —
// AI 분석이 실패해도 신고접수·출동 자체(FR-01/02 핵심 경로)는 막히면 안 되기 때문 (NFR-07 보조적 지위와도 일치).
@Component
public class AiAnalysisHttpAdapter implements AiAnalysisPort {

  private static final Logger log = LoggerFactory.getLogger(AiAnalysisHttpAdapter.class);

  private final RestClient restClient;

  public AiAnalysisHttpAdapter(RestClient.Builder restClientBuilder, org.springframework.core.env.Environment env) {
    String baseUrl = env.getProperty("faind.integration.ai-server.base-url", "http://localhost:8001");
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
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
