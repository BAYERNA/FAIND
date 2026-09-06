"""FR-24/26: YOLOv8 기반 화재·연기 감지, CCTV·드론 겸용 에이전트.

그래프: acquire_frame → run_detection
이 에이전트는 순수하게 "감지됐는가"만 판단한다 — Java backend로의 콜백(incident 생성,
drone recon 결과 기록)은 부수효과이므로 api/v1/fire_detection_router.py가 결과를 보고
직접 수행한다 (에이전트를 순수 함수에 가깝게 유지해 단위 테스트를 쉽게 한다).
"""

import logging
from typing import Optional

from langgraph.graph import END, StateGraph

from app.agents.base_agent import BaseAgent
from app.services import opencv_processor
from app.services.cctv_stream_service import CctvStreamService
from app.services.yolo_service import YoloService

logger = logging.getLogger(__name__)


class FireDetectionAgent(BaseAgent):
    def __init__(self, yolo_service: Optional[YoloService] = None, stream_service: Optional[CctvStreamService] = None):
        self.yolo_service = yolo_service or YoloService()
        self.stream_service = stream_service or CctvStreamService()
        self.graph = self.build_graph()

    def build_graph(self):
        graph = StateGraph(dict)
        graph.add_node("acquire_frame", self._acquire_frame)
        graph.add_node("run_detection", self._run_detection)
        graph.set_entry_point("acquire_frame")
        graph.add_edge("acquire_frame", "run_detection")
        graph.add_edge("run_detection", END)
        return graph.compile()

    async def run(self, state: dict) -> dict:
        return await self.graph.ainvoke(state)

    async def _acquire_frame(self, state: dict) -> dict:
        frame = None
        if state.get("image_base64"):
            frame = opencv_processor.decode_base64_image(state["image_base64"])
        elif state.get("image_url"):
            frame = opencv_processor.fetch_image_from_url(state["image_url"])
        elif state.get("stream_url"):
            frame = self.stream_service.capture_frame(state["stream_url"])

        if frame is None:
            state["frame"] = None
            state["acquire_error"] = "이미지를 획득하지 못했습니다 (image_base64/image_url/stream_url 확인 필요)."
            return state

        state["frame"] = opencv_processor.resize_for_inference(frame)
        return state

    async def _run_detection(self, state: dict) -> dict:
        frame = state.get("frame")
        if frame is None:
            state["detected"] = False
            state["confidence"] = 0.0
            state["label"] = None
            state["reason"] = state.get("acquire_error", "알 수 없는 오류로 감지를 수행하지 못했습니다.")
            return state

        result = self.yolo_service.detect_fire(frame)
        state["detected"] = result.detected
        state["confidence"] = result.confidence
        state["label"] = result.label
        state["reason"] = result.reason
        return state
