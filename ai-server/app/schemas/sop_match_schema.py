"""FR-08 SOP 교차 검증. backend의 SopMatchRequestDto / SopMatchResultDto와 매칭.

USR-003 "내 보고서 기록 / 실행 여부 / 추천 SOP 근거" 3열 표(NFR-07: AI 제안 vs 실제 행동 대조)를
그대로 채울 수 있는 구조로 sop_match_result를 만든다.
"""

from typing import Any, Optional
from uuid import UUID

from app.core.camel_model import CamelModel


class SopMatchRequest(CamelModel):
    report_id: UUID
    report_content: Optional[str] = None
    incident_id: Optional[UUID] = None


class SopMatchResponse(CamelModel):
    sop_match_result: dict[str, Any]
    risk_pattern: Optional[str] = None
    recommendation: Optional[str] = None
