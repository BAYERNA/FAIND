package com.faind.domain.report.service;

import com.faind.integration.ai.AiAnalysisPort;
import com.faind.integration.ai.dto.SopMatchRequestDto;
import com.faind.integration.ai.dto.SopMatchResultDto;
import java.util.UUID;
import org.springframework.stereotype.Component;

// FR-08: 제출된 보고서를 ai-server의 SOP 대조 에이전트로 보내는 report 패키지 전용 얇은 래퍼.
// AiAnalysisPort를 report 패키지 관점의 이름으로 감싸, ReportService가 integration/ai를 직접 몰라도 되게 한다.
@Component
public class SopMatchClient {

  private final AiAnalysisPort aiAnalysisPort;

  public SopMatchClient(AiAnalysisPort aiAnalysisPort) {
    this.aiAnalysisPort = aiAnalysisPort;
  }

  public SopMatchResultDto match(UUID reportId, String reportContent, UUID incidentId) {
    return aiAnalysisPort.requestSopMatch(new SopMatchRequestDto(reportId, reportContent, incidentId));
  }
}
