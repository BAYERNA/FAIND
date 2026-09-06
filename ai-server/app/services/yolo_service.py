"""YOLOv8 기반 화재·연기 감지 추론 엔진 (FR-24/26). CCTV·드론 양쪽이 이 서비스 하나를 재사용한다
(기술스택 §2.8: "CCTV 자동감지와 드론 정찰 양쪽에 동일 모델을 재사용해 학습·운영 비용을 줄인다").

중요한 정직성 원칙: 공개 배포되는 기본 YOLOv8 가중치(yolov8n.pt 등)는 COCO 데이터셋(사람·차량 등
80종)으로 학습된 것이라 "fire"/"smoke" 클래스가 없다. 화재 감지가 실제로 동작하려면 화재/연기
이미지로 파인튜닝한 가중치 파일이 FAIND_YOLO_MODEL_PATH에 있어야 한다. 그 파일이 없거나 클래스가
없으면, 조용히 거짓 양성/음성을 만들지 않고 "감지 불가" 상태를 명확히 반환한다.
"""

import logging
from dataclasses import dataclass
from typing import Optional

import numpy as np

from app.core.config import get_settings

logger = logging.getLogger(__name__)

FIRE_KEYWORDS = ("fire", "smoke", "flame", "화재", "연기")


@dataclass
class DetectionResult:
    detected: bool
    confidence: float
    label: Optional[str] = None
    reason: Optional[str] = None


class YoloService:
    def __init__(self, model_path: Optional[str] = None, confidence_threshold: Optional[float] = None):
        settings = get_settings()
        self._model_path = model_path or settings.yolo_model_path
        self._confidence_threshold = (
            confidence_threshold if confidence_threshold is not None else settings.fire_confidence_threshold
        )
        self._device = settings.yolo_device
        self._model = None
        self._fire_class_ids: set[int] = set()
        self._load_model()

    @property
    def is_available(self) -> bool:
        return self._model is not None and bool(self._fire_class_ids)

    def _load_model(self) -> None:
        try:
            from ultralytics import YOLO
        except ImportError:
            logger.warning("ultralytics가 설치되어 있지 않습니다 — 화재감지 기능이 비활성화됩니다.")
            return

        import os

        if not os.path.exists(self._model_path):
            logger.warning(
                "화재/연기 파인튜닝 모델(%s)을 찾을 수 없습니다 — 화재감지 기능이 비활성화됩니다. "
                "FAIND_YOLO_MODEL_PATH로 파인튜닝된 가중치 경로를 지정하세요.",
                self._model_path,
            )
            return

        try:
            model = YOLO(self._model_path)
        except Exception as e:
            logger.error("YOLO 모델 로드 실패 (%s): %s", self._model_path, e)
            return

        class_names = getattr(model, "names", {}) or {}
        fire_class_ids = {
            class_id
            for class_id, name in class_names.items()
            if any(keyword in str(name).lower() for keyword in FIRE_KEYWORDS)
        }
        if not fire_class_ids:
            logger.warning(
                "모델(%s)에 fire/smoke 클래스가 없습니다 — COCO 등 일반 목적 가중치로 보입니다. "
                "화재/연기로 파인튜닝된 모델이 아니면 화재감지 결과는 항상 미검출로 처리됩니다.",
                self._model_path,
            )
            self._model = model
            return

        self._model = model
        self._fire_class_ids = fire_class_ids
        logger.info("YOLO 화재감지 모델 로드 완료: %s (클래스 %s)", self._model_path, fire_class_ids)

    def detect_fire(self, frame: np.ndarray) -> DetectionResult:
        if not self.is_available:
            return DetectionResult(
                detected=False,
                confidence=0.0,
                reason="화재/연기 감지 모델이 준비되지 않았습니다 (모델 없음 또는 fire/smoke 클래스 없음).",
            )

        results = self._model.predict(frame, device=self._device, verbose=False)
        best_confidence = 0.0
        best_label = None
        for result in results:
            boxes = getattr(result, "boxes", None)
            if boxes is None:
                continue
            for box in boxes:
                class_id = int(box.cls[0])
                if class_id not in self._fire_class_ids:
                    continue
                confidence = float(box.conf[0])
                if confidence > best_confidence:
                    best_confidence = confidence
                    best_label = str(self._model.names.get(class_id, class_id))

        detected = best_confidence >= self._confidence_threshold
        return DetectionResult(detected=detected, confidence=round(best_confidence, 4), label=best_label)
