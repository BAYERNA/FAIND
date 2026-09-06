"""FR-24/26 CMD-002 라이브 카메라 뷰 — 순수 영상 중계(relay)만 담당한다.

위험 판단·경고는 이 엔드포인트의 책임이 아니다 — 그건 여전히 CCTV 폴링(/fire-detection/analyze)
경로에서만 나온다. 여기서 감지 박스를 얹어 그리기 시작하면 "이 화면에 박스가 없으면 안전하다"는
잘못된 신호를 줄 위험이 있어, 영상과 판단을 의도적으로 분리했다(Phase 3에서 위험 알림을 별도
채널로 붙일 예정).

프런트엔드가 `<img src="...">`로 직접 소비하는 걸 전제로 multipart/x-mixed-replace로 응답한다.
"""

import asyncio
import logging
import os
from typing import AsyncGenerator

import cv2
from fastapi import APIRouter, Query
from fastapi.responses import StreamingResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/streams", tags=["streams"])

JPEG_QUALITY = 70
FRAME_INTERVAL_SECONDS = 0.05  # 뷰어 화면 갱신 속도 상한(~20fps) 겸 서버 부하 조절
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
