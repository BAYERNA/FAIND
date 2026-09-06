"""CCTV/드론 영상 스트림에서 프레임을 추출한다.

데모 규모에서는 실제 카메라 하드웨어가 없으므로, RTSP/HTTP 스트림·로컬 이미지 파일 경로를
모두 같은 인터페이스로 받는다. APScheduler 기반 폴링(ENABLE_CCTV_POLLING)이나 수동 테스트 양쪽에서
동일하게 재사용한다.
"""

import logging
import os
import time
from typing import Optional

import cv2
import numpy as np

logger = logging.getLogger(__name__)


class CctvStreamService:
    def capture_frame(self, stream_url: str) -> Optional[np.ndarray]:
        if not stream_url:
            return None

        if os.path.exists(stream_url):
            frame = cv2.imread(stream_url)
            if frame is None:
                logger.warning("로컬 이미지 파일을 읽을 수 없습니다: %s", stream_url)
            return frame

        capture = cv2.VideoCapture(stream_url)
        try:
            if not capture.isOpened():
                logger.warning("스트림을 열 수 없습니다: %s", stream_url)
                return None
            ok, frame = capture.read()
            if not ok:
                logger.warning("스트림에서 프레임을 읽지 못했습니다: %s", stream_url)
                return None
            return frame
        finally:
            capture.release()

    def capture_burst(
        self, stream_url: str, frame_count: int = 5, interval_seconds: float = 0.15
    ) -> list[np.ndarray]:
        """짧은 시간(약 1초 이내) 동안 프레임 여러 장을 연속으로 받아온다.

        폴링 주기(기본 30초) 자체가 아니라, 폴링 "한 번" 안에서 찰나의 밝기 변화를 보기 위함이다
        — YoloService의 깜빡임 기반 오탐 필터가 이 프레임들을 근거로 "정적인 오탐 vs 진짜 불꽃"을
        판단한다. 정지 이미지 파일은 실제로 깜빡이지 않으므로 여러 장 복제해봐야 의미가 없어 1장만
        반환한다(깜빡임 판정은 자연히 "판단 보류"로 처리됨 — 실제로 없는 신호를 만들어내지 않는다).
        """
        if not stream_url:
            return []

        if os.path.exists(stream_url):
            frame = cv2.imread(stream_url)
            if frame is None:
                logger.warning("로컬 이미지 파일을 읽을 수 없습니다: %s", stream_url)
                return []
            return [frame]

        capture = cv2.VideoCapture(stream_url)
        frames: list[np.ndarray] = []
        try:
            if not capture.isOpened():
                logger.warning("스트림을 열 수 없습니다: %s", stream_url)
                return []
            for _ in range(frame_count):
                ok, frame = capture.read()
                if not ok:
                    break
                frames.append(frame)
                time.sleep(interval_seconds)
            if not frames:
                logger.warning("스트림에서 프레임을 읽지 못했습니다: %s", stream_url)
            return frames
        finally:
            capture.release()
