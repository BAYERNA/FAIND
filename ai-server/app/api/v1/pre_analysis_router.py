"""FR-02: backend IncidentCreatedListener → AiAnalysisHttpAdapter가 호출하는 엔드포인트."""

from fastapi import APIRouter, Depends

from app.agents.pre_analysis_agent import PreAnalysisAgent
from app.core.llm_factory import get_llm
from app.schemas.pre_analysis_schema import PreAnalysisRequest, PreAnalysisResponse

router = APIRouter(prefix="/pre-analysis", tags=["pre-analysis"])


def get_pre_analysis_agent() -> PreAnalysisAgent:
    return PreAnalysisAgent(llm=get_llm())


@router.post("", response_model=PreAnalysisResponse)
async def analyze(request: PreAnalysisRequest, agent: PreAnalysisAgent = Depends(get_pre_analysis_agent)):
    result = await agent.run({"incident_id": str(request.incident_id), "address": request.address})
    return PreAnalysisResponse(
        building_info=result.get("building_info", {}),
        hazard_info=result.get("hazard_info", {}),
        fire_history_info=result.get("fire_history_info", {}),
    )
