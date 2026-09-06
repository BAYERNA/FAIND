"""LLM 추상화. 기술스택 §2.2: "필요 시 GPT/Claude/Gemini 교체" 가능하도록 팩토리 뒤에 숨긴다.

에이전트는 이 함수가 반환하는 LangChain BaseChatModel(또는 None)만 알고, 어떤 벤더인지는 모른다.
provider가 none이거나 API 키가 없으면 None을 반환하고, 각 에이전트는 이를 "AI 보조 불가 —
휴리스틱 폴백"의 신호로 사용한다 (NFR-07 AI 보조적 지위 원칙: 없어도 핵심 계약은 지켜야 한다).
"""

import logging
from functools import lru_cache
from typing import Optional

from langchain_core.language_models import BaseChatModel

from app.core.config import get_settings

logger = logging.getLogger(__name__)


@lru_cache
def get_llm() -> Optional[BaseChatModel]:
    settings = get_settings()

    if settings.llm_provider == "anthropic" and settings.anthropic_api_key:
        from langchain_anthropic import ChatAnthropic

        return ChatAnthropic(
            model=settings.llm_model,
            api_key=settings.anthropic_api_key,
            timeout=15.0,
            max_retries=1,
        )

    logger.info("LLM_PROVIDER=%s — LLM 미설정, 에이전트는 휴리스틱 폴백으로 동작합니다.", settings.llm_provider)
    return None
