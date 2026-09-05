package com.faind.integration.ai;

import com.faind.integration.ai.dto.PreAnalysisRequestDto;
import com.faind.integration.ai.dto.PreAnalysisResultDto;
import com.faind.integration.ai.dto.SopMatchRequestDto;
import com.faind.integration.ai.dto.SopMatchResultDto;

// 코드구조설계서 §2 "왜 integration/을 따로 뒀는가" — incident/report 패키지는 이 인터페이스만
// 알고, Python AI서버와의 실제 통신(HTTP)은 AiAnalysisHttpAdapter가 캡슐화한다.
// 실제 MSA 분리 시 이 인터페이스는 그대로 두고 Adapter만 OpenFeign 클라이언트로 교체하면 된다.
public interface AiAnalysisPort {

  PreAnalysisResultDto requestPreAnalysis(PreAnalysisRequestDto request);

  SopMatchResultDto requestSopMatch(SopMatchRequestDto request);
}
