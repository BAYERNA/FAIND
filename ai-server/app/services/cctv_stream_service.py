"""CCTV/드론 영상 스트림에서 프레임을 추출한다.

데모 규모에서는 실제 카메라 하드웨어가 없으므로, RTSP/HTTP 스트림·로컬 이미지 파일 경로를
모두 같은 인터페이스로 받는다. APScheduler 기반 폴링(ENABLE_CCTV_POLLING)이나 수동 테스트 양쪽에서
동일하게 재사용한다.
"""

import logging
import os
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
