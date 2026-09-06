"""전역 설정. 기술스택 §2.2: Python 3.11 + FastAPI + LangGraph.

값은 전부 환경변수로 오버라이드 가능하며(pydantic-settings), 로컬 개발 기본값은
docker-compose.yml / backend의 application.yml과 짝을 맞췄다.
"""

import json
from functools import lru_cache
from typing import Optional

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class CameraConfig(BaseSettings):
    device_id: str
    stream_url: str


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="FAIND_", env_file=".env", extra="ignore")

    # FR-24/26 콜백 대상 — Java 모놀리식(backend). DispatchController.receiveCctvDetection,
    # IncidentController.recordDroneReconResult 참조.
    backend_base_url: str = "http://localhost:8080"
    backend_service_token: Optional[str] = None
    backend_request_timeout_seconds: float = 5.0

    # FR-02 사전분석 — 소방청 공공데이터포털.
    public_data_base_url: str = "https://apis.data.go.kr"
    public_data_service_key: str = ""
    public_data_request_timeout_seconds: float = 3.0

    # core/llm_factory.py — 미설정 시 모든 에이전트는 휴리스틱 폴백으로 동작한다(NFR-07 보조적 지위와
    # 같은 원칙: LLM이 없어도 핵심 API 계약은 항상 응답을 반환해야 한다).
    llm_provider: str = "none"  # none | anthropic
    anthropic_api_key: Optional[str] = None
    llm_model: str = "claude-sonnet-5"

    # FR-24/26 화재감지 임계값. 와이어프레임 ADM-001 예시(91%, 76%)를 참고해 기본값을 잡았다.
    yolo_model_path: str = "models/fire_yolov8.pt"
    yolo_device: str = "cpu"
    fire_confidence_threshold: float = 0.55

    # 데모 규모의 CCTV 자동 폴링(선택 기능). 기본은 꺼져 있고, 켤 경우 아래 JSON 배열로 카메라를 정의.
    # 예: FAIND_CCTV_POLLING_ENABLED=true FAIND_CCTV_CAMERAS='[{"device_id":"...","stream_url":"..."}]'
    cctv_polling_enabled: bool = False
    cctv_poll_interval_seconds: int = 30
    cctv_cameras_raw: str = Field(default="[]", alias="FAIND_CCTV_CAMERAS")

    cors_allowed_origins: list[str] = ["http://localhost:8080"]

    @field_validator("cors_allowed_origins", mode="before")
    @classmethod
    def _split_origins(cls, value):
        if isinstance(value, str):
            return [origin.strip() for origin in value.split(",") if origin.strip()]
        return value

    @property
    def cctv_cameras(self) -> list[CameraConfig]:
        try:
            raw = json.loads(self.cctv_cameras_raw)
        except json.JSONDecodeError:
            return []
        return [CameraConfig(**item) for item in raw]


@lru_cache
def get_settings() -> Settings:
    return Settings()
