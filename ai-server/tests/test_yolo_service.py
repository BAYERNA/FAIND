"""yolo_service.py의 핵심 판단 로직(위험도 점수·등급, 확산 추적) 회귀 방지 테스트.

detect_fire_burst() 자체(모델 추론 결과 파싱)는 실제 YOLO 모델 로드가 필요해 여기서는 건드리지
않는다 — 대신 모델과 무관하게 순수 계산인 _score_danger/_level_for_score/_track_spread를
직접 겨냥한다. 이 세 개가 바로 Phase 1에서 "정직성 원칙"에 따라 새로 설계한 위험도 판단의
핵심이라, 값이 조금이라도 어긋나면 CMD-002/ADM-010의 배지·정렬·자동경고가 전부 틀어진다.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.services.yolo_service import (  # noqa: E402
    DANGER_SCORE_CRITICAL,
    DANGER_SCORE_DANGER,
    DANGER_SCORE_WARNING,
    GROWTH_ALERT_RATIO,
    YoloService,
)


def make_service() -> YoloService:
    # 모델 파일이 없어도 __init__은 안전하게 끝난다(is_available=False) — 여기서 테스트하는
    # 메서드들은 모델 추론과 무관한 순수 계산이라 그걸로 충분하다.
    return YoloService(model_path="/nonexistent/model.pt")


class TestLevelForScore:
    def test_경계값_미만은_한_단계_낮은_등급이다(self):
        service = make_service()
        assert service._level_for_score(DANGER_SCORE_WARNING - 0.1) == "SAFE"
        assert service._level_for_score(DANGER_SCORE_DANGER - 0.1) == "WARNING"
        assert service._level_for_score(DANGER_SCORE_CRITICAL - 0.1) == "DANGER"

    def test_경계값_이상은_해당_등급이다(self):
        service = make_service()
        assert service._level_for_score(DANGER_SCORE_WARNING) == "WARNING"
        assert service._level_for_score(DANGER_SCORE_DANGER) == "DANGER"
        assert service._level_for_score(DANGER_SCORE_CRITICAL) == "CRITICAL"

    def test_0점은_안전이다(self):
        service = make_service()
        assert service._level_for_score(0.0) == "SAFE"


class TestScoreDanger:
    def test_fire가_smoke보다_점수가_높다(self):
        service = make_service()
        fire_score = service._score_danger(0.9, 0.3, "fire", None, None)
        smoke_score = service._score_danger(0.9, 0.3, "smoke", None, None)
        assert fire_score > smoke_score

    def test_면적이_클수록_점수가_높다(self):
        service = make_service()
        small_area = service._score_danger(0.9, 0.01, "fire", None, None)
        large_area = service._score_danger(0.9, 0.3, "fire", None, None)
        assert large_area > small_area

    def test_확산_추세가_있으면_점수가_올라간다(self):
        service = make_service()
        base = service._score_danger(0.9, 0.1, "fire", None, None)
        growing = service._score_danger(0.9, 0.1, "fire", GROWTH_ALERT_RATIO, None)
        assert growing > base

    def test_깜빡임_미검증시_오탐의심이면_점수가_절반으로_낮아진다(self):
        service = make_service()
        verified_unknown = service._score_danger(0.9, 0.3, "fire", None, None)
        flicker_false = service._score_danger(0.9, 0.3, "fire", None, False)
        assert flicker_false == verified_unknown * 0.5

    def test_점수는_100을_넘지_않는다(self):
        service = make_service()
        score = service._score_danger(1.0, 1.0, "fire", 5.0, None)
        assert score <= 100.0


class TestTrackSpread:
    def test_첫_관측은_비교_대상이_없어_판단_보류다(self):
        service = make_service()
        growth_ratio, spread_direction, spread_speed = service._track_spread("cam-1", 0.1, (0, 0, 10, 10))
        assert growth_ratio is None
        assert spread_direction is None
        assert spread_speed is None

    def test_카메라별_이력은_서로_섞이지_않는다(self, monkeypatch):
        # Phase 1에서 명시적으로 피한 버그(원본 참고 코드 6번 파일의 결함): 여러 카메라를 같은
        # YoloService 인스턴스가 서비스할 때, 한 카메라의 관측 이력이 다른 카메라로 새면 안 된다.
        service = make_service()
        fake_time = [1000.0]
        monkeypatch.setattr("app.services.yolo_service.time.time", lambda: fake_time[0])

        service._track_spread("cam-1", 0.5, (0, 0, 100, 100))  # cam-1만 큰 화재로 시작
        service._track_spread("cam-2", 0.01, (0, 0, 10, 10))  # cam-2는 작은 화재로 시작

        fake_time[0] += 2.0  # MIN_GROWTH_INTERVAL_SECONDS(1.0s)를 넘겨야 growth_ratio가 계산됨
        growth_cam2, _, _ = service._track_spread("cam-2", 0.02, (0, 0, 14, 14))

        # cam-2의 growth_ratio가 cam-1의 훨씬 큰 area_ratio(0.5)를 기준으로 계산됐다면 이 값이
        # 크게 어긋난다 — cam-2 자신의 첫 관측(0.01) 대비로만 계산돼야 한다.
        assert growth_cam2 == 2.0  # 0.02 / 0.01

    def test_면적이_늘고_문턱값을_넘으면_확산_방향이_찍힌다(self, monkeypatch):
        service = make_service()
        fake_time = [1000.0]
        monkeypatch.setattr("app.services.yolo_service.time.time", lambda: fake_time[0])

        service._track_spread("cam-1", 0.1, (0, 0, 100, 100))  # 중심 (50, 50)
        fake_time[0] += 2.0
        growth_ratio, spread_direction, spread_speed = service._track_spread(
            "cam-1", 0.1 * GROWTH_ALERT_RATIO, (50, 50, 150, 150)  # 중심 (100, 100) — 오른쪽 아래로 이동
        )

        assert growth_ratio == GROWTH_ALERT_RATIO
        assert spread_direction == "→↓"
        assert spread_speed is not None and spread_speed > 0

    def test_문턱값_미만_증가는_방향을_찍지_않는다(self, monkeypatch):
        service = make_service()
        fake_time = [1000.0]
        monkeypatch.setattr("app.services.yolo_service.time.time", lambda: fake_time[0])

        service._track_spread("cam-1", 0.1, (0, 0, 100, 100))
        fake_time[0] += 2.0
        growth_ratio, spread_direction, _ = service._track_spread("cam-1", 0.105, (0, 0, 100, 100))

        assert growth_ratio < GROWTH_ALERT_RATIO
        assert spread_direction is None
