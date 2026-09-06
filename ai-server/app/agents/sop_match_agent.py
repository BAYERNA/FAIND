"""FR-08: 제출된 사후보고서를 SOP 체크리스트와 대조하는 에이전트.

그래프: match_against_checklist → generate_summary
USR-003 "내 보고서 기록 / 실행 여부 / 추천 SOP 근거" 3열 표(NFR-07 "AI 제안 vs 실제 행동 대조")를
그대로 채울 수 있도록 sop_match_result를 리스트[dict] 구조로 만든다.

SOP_CHECKLIST는 데모용 고정 목록이다 — 실서비스 단계에서는 기관별 SOP 문서를 벡터 검색으로
대조하는 방식(RAG)으로 교체하되, 이 에이전트의 build_graph()/run() 계약은 그대로 유지된다.
"""

import logging
from typing import Any, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import HumanMessage
from langgraph.graph import END, StateGraph

from app.agents.base_agent import BaseAgent

logger = logging.getLogger(__name__)

SOP_CHECKLIST = [
    {"item": "화재 인지 → 무전 보고", "reference": "§4.2 무전 보고 원칙", "keywords": ["무전", "보고"]},
    {"item": "구역별 인명 검색", "reference": "§8.7 구역별 인명 검색 절차", "keywords": ["인명", "검색", "구조"]},
    {"item": "초기 진압 절차", "reference": "§3.1 초기 진압 절차", "keywords": ["진압", "방수", "소화"]},
    {"item": "대피 유도 절차", "reference": "§5.4 대피 유도 절차", "keywords": ["대피", "유도"]},
    {"item": "잔화 확인 및 철수", "reference": "§9.2 잔화 확인 절차", "keywords": ["잔화", "진화", "철수"]},
]


class SopMatchAgent(BaseAgent):
    def __init__(self, llm: Optional[BaseChatModel] = None):
        self.llm = llm
        self.graph = self.build_graph()

    def build_graph(self):
        graph = StateGraph(dict)
        graph.add_node("match_against_checklist", self._match_against_checklist)
        graph.add_node("generate_summary", self._generate_summary)
        graph.set_entry_point("match_against_checklist")
        graph.add_edge("match_against_checklist", "generate_summary")
        graph.add_edge("generate_summary", END)
        return graph.compile()

    async def run(self, state: dict) -> dict:
        return await self.graph.ainvoke(state)

    async def _match_against_checklist(self, state: dict) -> dict:
        content = (state.get("report_content") or "").lower()
        items = []
        for sop in SOP_CHECKLIST:
            executed = any(keyword.lower() in content for keyword in sop["keywords"])
            items.append(
                {
                    "reportRecord": sop["item"],
                    "executed": "일치" if executed else "미실행",
                    "recommendedSop": sop["reference"],
                }
            )
        state["sop_match_items"] = items
        return state

    async def _generate_summary(self, state: dict) -> dict:
        items = state["sop_match_items"]
        missed = [i["reportRecord"] for i in items if i["executed"] == "미실행"]

        if self.llm and state.get("report_content"):
            risk_pattern, recommendation = await self._summarize_with_llm(state["report_content"], missed)
        else:
            risk_pattern, recommendation = self._summarize_heuristic(items, missed)

        state["sop_match_result"] = {"items": items, "matchedCount": len(items) - len(missed), "totalCount": len(items)}
        state["risk_pattern"] = risk_pattern
        state["recommendation"] = recommendation
        return state

    async def _summarize_with_llm(self, report_content: str, missed: list[str]) -> tuple[str, str]:
        prompt = (
            "다음은 화재 대응 사후보고서다. 놓친 SOP 항목을 참고해 위험 패턴 한 문장과 "
            f"개선 권고 한 문장을 각각 작성하라(줄바꿈으로 구분).\n보고서: {report_content}\n"
            f"놓친 항목: {', '.join(missed) if missed else '없음'}"
        )
        try:
            response = await self.llm.ainvoke([HumanMessage(content=prompt)])
            lines = [line.strip() for line in response.content.strip().splitlines() if line.strip()]
            risk_pattern = lines[0] if lines else None
            recommendation = lines[1] if len(lines) > 1 else None
            return risk_pattern, recommendation
        except Exception as e:
            logger.warning("LLM SOP 요약 실패, 휴리스틱으로 대체: %s", e)
            return self._summarize_heuristic_from_missed(missed)

    def _summarize_heuristic(self, items: list[dict[str, Any]], missed: list[str]) -> tuple[str, str]:
        return self._summarize_heuristic_from_missed(missed)

    def _summarize_heuristic_from_missed(self, missed: list[str]) -> tuple[str, str]:
        if not missed:
            return "SOP 체크리스트 전 항목을 준수했습니다.", "현재 대응 절차를 유지하십시오."
        risk_pattern = f"다음 절차가 기록에서 확인되지 않았습니다: {', '.join(missed)}."
        recommendation = f"'{missed[0]}' 절차를 포함해 대응 순서를 재정비하는 것을 권장합니다."
        return risk_pattern, recommendation
