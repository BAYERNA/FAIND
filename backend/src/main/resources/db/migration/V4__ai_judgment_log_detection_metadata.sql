-- Phase 6 ADM-009 통계: CCTV 자동/수동 감지 비율과 평균 위험도를 집계하려면 ai_judgment_logs에
-- 그 값이 구조화된 컬럼으로 남아 있어야 한다(지금까지는 summary 텍스트 안에만 있어 조회 불가).
-- 둘 다 CCTV_DETECTION(그리고 향후 DRONE_RECON) 유형에서만 채워지는 선택 값이라 NULL 허용.
ALTER TABLE ai_judgment_logs ADD COLUMN danger_score NUMERIC(5, 2);
ALTER TABLE ai_judgment_logs ADD COLUMN detection_source VARCHAR(10);
