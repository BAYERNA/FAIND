"""FR-24/26 CMD-002 라이브 카메라 뷰 — 영상 중계(relay)와 위험도 스냅샷을 함께 담당한다.

/mjpeg는 순수 영상만 내보낸다 — 여기서 감지 박스를 얹어 그리면 "이 화면에 박스가 없으면
안전하다"는 잘못된 신호를 줄 위험이 있어, 영상과 판단을 의도적으로 분리했다. 대신 /danger가
같은 카메라에 대한 위험도 판단(danger_level/score, 깜빡임 검증, 확산 신호)을 별도 채널로
내려준다 — 프런트가 영상 위에 겹쳐 그리지 않고 옆에 배지로만 표시하도록(코드구조설계서와
동일하게 판단·표시를 분리 유지).

프런트엔드가 `<img src="...">`로 직접 소비하는 걸 전제로 /mjpeg는 multipart/x-mixed-replace로 응답한다.
"""

import asyncio
import logging
import os
from typing import AsyncGenerator

import cv2
from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse

from app.agents.fire_detection_agent import FireDetectionAgent
from app.api.v1.fire_detection_router import _result_kwargs, get_fire_detection_agent
from app.schemas.fire_detection_schema import FireDetectionResult

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/streams", tags=["streams"])

JPEG_QUALITY = 70
FRAME_INTERVAL_SECONDS = 0.01  # 뷰어 화면 갱신 속도 상한(~100fps) 겸 서버 부하 조절 — 실제 상한은
# 어차피 capture.read()의 네트워크·디코딩 시간이 결정하므로, 여기서는 그 시간에 더는 보태지 않는
# 최소값만 준다.
STATIC_IMAGE_REPEAT_INTERVAL_SECONDS = 1.0  # 로컬 이미지 파일(테스트용)은 매초 그대로 재전송


async def _mjpeg_frames(stream_url: str) -> AsyncGenerator[bytes, None]:
    # cv2 호출은 전부 블로킹이라, 이벤트 루프를 막지 않도록 asyncio.to_thread로 스레드에 위임한다
    # (같은 워커가 다른 요청 - 예: /fire-detection/analyze - 도 동시에 처리해야 하므로).
    if os.path.exists(stream_url):
        frame = await asyncio.to_thread(cv2.imread, stream_url)
        if frame is None:
            logger.warning("정지 이미지를 읽을 수 없습니다: %s", stream_url)
            return
        while True:
            ok, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY])
            if ok:
                yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + buffer.tobytes() + b"\r\n"
            await asyncio.sleep(STATIC_IMAGE_REPEAT_INTERVAL_SECONDS)
        return

    capture = await asyncio.to_thread(cv2.VideoCapture, stream_url)
    try:
        # 실시간 IP 카메라(휴대폰 등)는 read()가 네트워크·디코딩 지연 없이 딱 최신 프레임만
        # 돌려주지 않고, 내부 버퍼에 여러 프레임을 쌓아뒀다가 순서대로 내보내는 경우가 있다 —
        # 그러면 시간이 갈수록 화면이 실제보다 뒤처져 보인다("점점 느려짐"). 버퍼를 최소로 둬서
        # 항상 최신에 가까운 프레임만 읽게 한다(백엔드가 이 옵션을 지원 안 하면 그냥 무시된다).
        await asyncio.to_thread(capture.set, cv2.CAP_PROP_BUFFERSIZE, 1)
        opened = await asyncio.to_thread(capture.isOpened)
        if not opened:
            logger.warning("스트림을 열 수 없습니다: %s", stream_url)
            return
        while True:
            ok, frame = await asyncio.to_thread(capture.read)
            if not ok:
                logger.warning("스트림에서 프레임을 읽지 못했습니다(연결 종료로 판단): %s", stream_url)
                break
            ok2, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY])
            if not ok2:
                continue
            yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + buffer.tobytes() + b"\r\n"
            await asyncio.sleep(FRAME_INTERVAL_SECONDS)
    finally:
        await asyncio.to_thread(capture.release)


@router.get("/mjpeg")
async def mjpeg_stream(
    stream_url: str = Query(..., description="CCTV/드론 스트림 주소 또는 로컬 이미지 경로(테스트용)"),
):
    return StreamingResponse(
        _mjpeg_frames(stream_url),
        media_type="multipart/x-mixed-replace; boundary=frame",
    )


@router.get("/danger", response_model=FireDetectionResult)
async def live_danger(
    stream_url: str = Query(..., description="CCTV/드론 스트림 주소 또는 로컬 이미지 경로(테스트용)"),
    device_id: str = Query(..., description="깜빡임·확산 이력을 카메라별로 구분하는 키(카메라 deviceId)"),
    agent: FireDetectionAgent = Depends(get_fire_detection_agent),
):
    # /fire-detection/analyze와 같은 agent(=같은 YoloService 인스턴스)를 쓴다 — 카메라별
    # 깜빡임·확산 이력(_camera_history)이 device_id로 이어지려면 인스턴스가 같아야 한다.
    # backend 콜백은 하지 않는다: 이 엔드포인트는 지휘관이 보고 있는 화면의 현재 위험도를
    # 읽기만 할 뿐, incident를 새로 만들거나 확정하지 않는다(그 경로는 여전히 CCTV 폴링뿐).
    result = await agent.run({"stream_url": stream_url, "device_id": device_id})
    return FireDetectionResult(reason=result.get("reason"), **_result_kwargs(result))
