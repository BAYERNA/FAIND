"""LangGraph 기반 에이전트 공통 계약 (Abstract Agent Base Class).

기술스택 §2.2: "LangGraph로 사전분석·SOP대조·화재감지 3개 에이전트를 독립 모듈로
오케스트레이션해, 에이전트 하나를 교체해도 나머지에 영향이 없다." — 그래서 이 계약은
build_graph()/run() 두 메서드만 강제하고, 각 에이전트 내부의 그래프 노드 구성은 서로 완전히
독립적으로 둔다.
"""

from abc import ABC, abstractmethod
from typing import Any


class BaseAgent(ABC):
    @abstractmethod
    def build_graph(self) -> Any:
        """LangGraph StateGraph를 구성하고 컴파일된 그래프를 반환한다."""
        raise NotImplementedError

    @abstractmethod
    async def run(self, state: dict) -> dict:
        """컴파일된 그래프를 실행해 최종 state를 반환한다."""
        raise NotImplementedError
