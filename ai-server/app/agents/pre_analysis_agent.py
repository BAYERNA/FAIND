"""FR-02: 신고 접수 즉시 공공데이터 기반 위험요소를 사전 판별하는 에이전트.

그래프: fetch_public_data → structure_result
NFR-03(3초 이내)을 지키기 위해 공공데이터 API 타임아웃을 짧게 두고, 실패해도 항상 구조화된
결과를 반환한다(휴리스틱 폴백) — "AI 분석이 실패해도 신고접수·출동 자체는 막히면 안 된다"는
backend AiAnalysisHttpAdapter의 회복탄력성 원칙을 AI서버 쪽에서도 그대로 지킨다.
"""

import hashlib
import logging
from typing import Any, Optional

import httpx
from langchain_core.language_models import BaseChatModel
from langchain_core.messages import HumanMessage
from langgraph.graph import END, StateGraph

from app.agents.base_agent import BaseAgent
from app.core.config import get_settings

logger = logging.getLogger(__name__)

# 건물 구조/위험물 종류를 주소 해시로 결정론적으로 골라, 같은 주소를 다시 조회해도 같은 데모
# 데이터가 나오게 한다. 실제 공공데이터 API 응답이 있을 때는 이 목록을 쓰지 않는다.
_STRUCTURE_SAMPLES = ["철근콘크리트", "경량철골조", "목구조", "조적조"]
_HAZARD_SAMPLES = [
    "인화성 물질 보관 · 2층 창고",
    "가스 저장시설 인접",
    "특이 위험물 없음",
    "전기실 노후 배선 이력",
]


class PreAnalysisAgent(BaseAgent):
    def __init__(self, llm: Optional[BaseChatModel] = None):
        self.llm = llm
        self.settings = get_settings()
        self.graph = self.build_graph()

    def build_graph(self):
        graph = StateGraph(dict)
        graph.add_node("fetch_public_data", self._fetch_public_data)
        graph.add_node("structure_result", self._structure_result)
        graph.set_entry_point("fetch_public_data")
        graph.add_edge("fetch_public_data", "structure_result")
        graph.add_edge("structure_result", END)
        return graph.compile()

    async def run(self, state: dict) -> dict:
        return await self.graph.ainvoke(state)

    async def _fetch_public_data(self, state: dict) -> dict:
        address = state.get("address", "")
        if not self.settings.public_data_service_key:
            state["raw_public_data"] = None
            return state
        try:
            async with httpx.AsyncClient(timeout=self.settings.public_data_request_timeout_seconds) as client:
                response = await client.get(
                    f"{self.settings.public_data_base_url}/1613000/BldRgstService_v2/getBrTitleInfo",
                    params={"serviceKey": self.settings.public_data_service_key, "address": address},
                )
                response.raise_for_status()
                state["raw_public_data"] = response.json()
        except Exception as e:
            logger.warning("공공데이터 API 조회 실패 (address=%s): %s — 휴리스틱으로 대체", address, e)
            state["raw_public_data"] = None
        return state

    async def _structure_result(self, state: dict) -> dict:
        raw = state.get("raw_public_data")
        address = state.get("address", "")

        if raw:
            structured = self._structure_from_raw(raw)
        elif self.llm:
            structured = await self._structure_with_llm(address)
        else:
            structured = self._structure_heuristic(address)

        state.update(structured)
        return state

    def _structure_from_raw(self, raw: dict) -> dict[str, Any]:
        # 실제 공공데이터 API 응답 스키마는 기관/엔드포인트별로 상이하므로, 여기서는 필요한
        # 필드만 방어적으로 뽑아 쓰고 나머지는 원본을 hazard_info에 보존한다.
        return {
            "building_info": raw.get("buildingInfo", raw),
            "hazard_info": raw.get("hazardInfo", {}),
            "fire_history_info": raw.get("fireHistoryInfo", {"recent_5years": "정보 없음"}),
        }

    async def _structure_with_llm(self, address: str) -> dict[str, Any]:
        prompt = (
            "다음 주소의 건물에 대해 화재 대응 사전분석용 정보를 JSON으로만 답하라. "
            '키는 building_info(구조/층수/준공연도), hazard_info(위험요소), '
            "fire_history_info(최근 5년 화재 이력)이다. 실제 데이터가 없다면 이 지역 평균적인 "
            f"추정치를 사용하라.\n주소: {address}"
        )
        try:
            response = await self.llm.ainvoke([HumanMessage(content=prompt)])
            import json

            return json.loads(response.content)
        except Exception as e:
            logger.warning("LLM 사전분석 실패, 휴리스틱으로 대체: %s", e)
            return self._structure_heuristic(address)

    def _structure_heuristic(self, address: str) -> dict[str, Any]:
        seed = int(hashlib.sha1(address.encode("utf-8")).hexdigest(), 16)
        structure = _STRUCTURE_SAMPLES[seed % len(_STRUCTURE_SAMPLES)]
        hazard = _HAZARD_SAMPLES[(seed // 7) % len(_HAZARD_SAMPLES)]
        floors = 2 + (seed % 15)
        built_year = 1985 + (seed % 40)
        return {
            "building_info": {
                "structure": structure,
                "floors": floors,
                "built_year": built_year,
                "source": "공공데이터 미연동 — 데모용 추정치",
            },
            "hazard_info": {"summary": hazard, "source": "공공데이터 미연동 — 데모용 추정치"},
            "fire_history_info": {"recent_5years": "이력 없음(추정)", "source": "공공데이터 미연동 — 데모용 추정치"},
        }
