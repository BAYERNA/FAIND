"""영상 프레임 전처리 (base64/URL 디코딩, 리사이즈). YOLO 추론 전 공통 단계."""

import base64
import logging
from typing import Optional

import cv2
import numpy as np

logger = logging.getLogger(__name__)

MAX_DIMENSION = 1280


def decode_base64_image(image_base64: str) -> Optional[np.ndarray]:
    try:
        # data URL 접두사("data:image/png;base64,...")가 붙어 있어도 처리한다.
        if "," in image_base64 and image_base64.strip().startswith("data:"):
            image_base64 = image_base64.split(",", 1)[1]
        raw = base64.b64decode(image_base64)
        array = np.frombuffer(raw, dtype=np.uint8)
        frame = cv2.imdecode(array, cv2.IMREAD_COLOR)
        return frame
    except Exception as e:
        logger.warning("base64 이미지 디코딩 실패: %s", e)
        return None


def fetch_image_from_url(image_url: str, timeout_seconds: float = 5.0) -> Optional[np.ndarray]:
    try:
        import httpx

        with httpx.Client(timeout=timeout_seconds) as client:
            response = client.get(image_url)
            response.raise_for_status()
            array = np.frombuffer(response.content, dtype=np.uint8)
            return cv2.imdecode(array, cv2.IMREAD_COLOR)
    except Exception as e:
        logger.warning("URL 이미지 조회 실패 (%s): %s", image_url, e)
        return None


def resize_for_inference(frame: np.ndarray, max_dimension: int = MAX_DIMENSION) -> np.ndarray:
    height, width = frame.shape[:2]
    longest_side = max(height, width)
    if longest_side <= max_dimension:
        return frame
    scale = max_dimension / longest_side
    return cv2.resize(frame, (int(width * scale), int(height * scale)), interpolation=cv2.INTER_AREA)
