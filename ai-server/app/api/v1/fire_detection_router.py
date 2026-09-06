"""FR-24(CCTV 자동 화재감지) / FR-26(드론 정찰). 감지 후 Java backend로 콜백까지 수행한다.

- CCTV: 감지되면 backend에 새 incident(AI_SUSPECTED)를 생성 요청한다 — NFR-08에 따라 이 호출은
  절대 정식 출동을 만들지 않는다. 확정은 오직 관제센터(ADM-001, role=ADMIN)만 할 수 있다.
- DRONE: 감지 여부와 무관하게 정찰 결과를 기존 drone_dispatches row에 기록만 한다(FR-26).
"""

import logging

from fastapi import APIRouter, Depends

from app.agents.fire_detection_agent import FireDetectionAgent
from app.core.backend_client import BackendClientError, FaindBackendClient
from app.schemas.fire_detection_schema import DetectionSourceType, FireDetectionRequest, FireDetectionResult
from app.services.yolo_service import YoloService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/fire-detection", tags=["fire-detection"])

# YOLO 모델 로드는 비용이 크므로 프로세스당 한 번만 수행한다.
_shared_yolo_service = YoloService()


def get_fire_detection_agent() -> FireDetectionAgent:
    return FireDetectionAgent(yolo_service=_shared_yolo_service)


def get_backend_client() -> FaindBackendClient:
    return FaindBackendClient()


@router.post("/analyze", response_model=FireDetectionResult)
async def analyze(
    request: FireDetectionRequest,
    agent: FireDetectionAgent = Depends(get_fire_detection_agent),
    backend_client: FaindBackendClient = Depends(get_backend_client),
):
    result = await agent.run(
        {"image_base64": request.image_base64, "image_url": request.image_url}
    )

    detected = result.get("detected", False)
    confidence = result.get("confidence", 0.0)
    reason = result.get("reason")
    label = result.get("label")

    if not (detected and request.notify_backend):
        return FireDetectionResult(detected=detected, confidence=confidence, label=label, reason=reason)

    if request.source_type == DetectionSourceType.CCTV:
        try:
            incident_id = await backend_client.report_cctv_detection(
                camera_device_id=request.device_id,
                confidence_score=confidence * 100,
                summary=f"YOLOv8 화재/연기 감지 (label={label}, confidence={confidence:.2f})",
                address_hint=request.address_hint,
            )
            return FireDetectionResult(
                detected=True, confidence=confidence, label=label, callback_sent=True, incident_id=incident_id
            )
        except BackendClientError as e:
            return FireDetectionResult(
                detected=True, confidence=confidence, label=label, callback_sent=False, reason=str(e)
            )

    # DRONE
    if request.dispatch_id is None:
        return FireDetectionResult(
            detected=True, confidence=confidence, label=label, callback_sent=False,
            reason="DRONE 소스는 dispatchId가 필요합니다.",
        )
    try:
        await backend_client.report_drone_recon(
            dispatch_id=request.dispatch_id,
            confidence_score=confidence * 100,
            summary=f"드론 정찰 화재/연기 감지 (label={label}, confidence={confidence:.2f})",
        )
        return FireDetectionResult(detected=True, confidence=confidence, label=label, callback_sent=True)
    except BackendClientError as e:
        return FireDetectionResult(detected=True, confidence=confidence, label=label, callback_sent=False, reason=str(e))
