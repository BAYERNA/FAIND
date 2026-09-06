"""FR-08: backend ReportService.SopMatchClient → AiAnalysisHttpAdapter가 호출하는 엔드포인트."""

from fastapi import APIRouter, Depends

from app.agents.sop_match_agent import SopMatchAgent
from app.core.llm_factory import get_llm
from app.schemas.sop_match_schema import SopMatchRequest, SopMatchResponse

router = APIRouter(prefix="/sop-match", tags=["sop-match"])


def get_sop_match_agent() -> SopMatchAgent:
    return SopMatchAgent(llm=get_llm())


@router.post("", response_model=SopMatchResponse)
async def match(request: SopMatchRequest, agent: SopMatchAgent = Depends(get_sop_match_agent)):
    result = await agent.run({"report_content": request.report_content or ""})
    return SopMatchResponse(
        sop_match_result=result.get("sop_match_result", {}),
        risk_pattern=result.get("risk_pattern"),
        recommendation=result.get("recommendation"),
    )
