"""api/v1 라우터 조립. app/main.py가 이 하나만 include한다."""

from fastapi import APIRouter

from app.api.v1 import fire_detection_router, pre_analysis_router, sop_match_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(pre_analysis_router.router)
api_router.include_router(sop_match_router.router)
api_router.include_router(fire_detection_router.router)
