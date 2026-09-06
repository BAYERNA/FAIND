"""Java 백엔드(Jackson 기본 camelCase 직렬화)와 JSON 계약을 맞추기 위한 공용 베이스 모델.

integration/ai/dto의 PreAnalysisRequestDto(incidentId, ...) 같은 Java record는 camelCase로
직렬화되므로, 여기서도 snake_case 필드를 camelCase 별칭으로 노출해 양쪽이 그대로 맞물리게 한다.
"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
    )
