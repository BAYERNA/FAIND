"""FR-02 사전분석. backend의 PreAnalysisRequestDto / PreAnalysisResultDto와 1:1로 맞춘 계약.

NFR-03: 신고 접수 후 3초 이내 표시가 목표이므로, 이 계약은 항상 응답을 반환해야 한다
(공공데이터 API·LLM이 실패해도 pre_analysis_agent가 휴리스틱으로 채워 넣는다).
"""

from typing import Any, Optional
from uuid import UUID

from app.core.camel_model import CamelModel


class PreAnalysisRequest(CamelModel):
    incident_id: UUID
    address: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class PreAnalysisResponse(CamelModel):
    building_info: dict[str, Any]
    hazard_info: dict[str, Any]
    fire_history_info: dict[str, Any]
