"""Java 모놀리식(backend)으로 나가는 콜백 호출. 코드구조설계서 §3 fire_detection_router.py
"감지결과 수신 + Java 모놀리식으로 콜백" 부분의 실제 구현.

FR-24: CCTV 의심감지 → DispatchController.receiveCctvDetection (AI_SUSPECTED 생성)
FR-26: 드론 정찰 결과 → IncidentController.recordDroneReconResult (ai_judgment_logs 적재)

Java 쪽 NFR-08 오탐 방지 게이트는 여기서 강제하지 않는다 — 이 호출은 항상 "의심됨"만 보고할 뿐,
정식 출동 전환(role=ADMIN 확인)은 Java IncidentConfirmService만이 수행한다는 원칙을 그대로 지킨다.
"""

import logging
from typing import Optional
from uuid import UUID

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class BackendClientError(Exception):
    pass


class FaindBackendClient:
    def __init__(self, base_url: Optional[str] = None, timeout: Optional[float] = None, token: Optional[str] = None):
        settings = get_settings()
        self._base_url = (base_url or settings.backend_base_url).rstrip("/")
        self._timeout = timeout or settings.backend_request_timeout_seconds
        self._token = token if token is not None else settings.backend_service_token

    def _headers(self) -> dict:
        headers = {"Content-Type": "application/json"}
        if self._token:
            headers["Authorization"] = f"Bearer {self._token}"
        return headers

    async def report_cctv_detection(
        self,
        camera_device_id: UUID,
        confidence_score: float,
        summary: str,
        address_hint: Optional[str] = None,
    ) -> UUID:
        """FR-24: CCTV 화재 의심 감지를 Java로 보고하고, 생성된 incident_id를 받는다."""
        payload = {
            "cameraDeviceId": str(camera_device_id),
            "addressHint": address_hint,
            "confidenceScore": confidence_score,
            "summary": summary,
        }
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            try:
                response = await client.post(
                    f"{self._base_url}/api/v1/incidents/dispatch/cctv-detections",
                    json=payload,
                    headers=self._headers(),
                )
                response.raise_for_status()
                return UUID(response.json())
            except httpx.HTTPError as e:
                logger.error("FR-24 CCTV 감지 콜백 실패 (camera=%s): %s", camera_device_id, e)
                raise BackendClientError(str(e)) from e

    async def report_drone_recon(
        self, dispatch_id: UUID, confidence_score: float, summary: str, video_ref: Optional[str] = None
    ) -> None:
        """FR-26: 드론 정찰 결과를 Java로 보고한다."""
        payload = {"confidenceScore": confidence_score, "summary": summary, "videoRef": video_ref}
        async with httpx.AsyncClient(timeout=self._timeout) as client:
            try:
                response = await client.post(
                    f"{self._base_url}/api/v1/incidents/drone-dispatches/{dispatch_id}/recon-result",
                    json=payload,
                    headers=self._headers(),
                )
                response.raise_for_status()
            except httpx.HTTPError as e:
                logger.error("FR-26 드론 정찰 콜백 실패 (dispatch=%s): %s", dispatch_id, e)
                raise BackendClientError(str(e)) from e
