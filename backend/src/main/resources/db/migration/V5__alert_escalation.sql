-- Phase 7: FR-22 위험경고 미확인 에스컬레이션. 최초 발송(sent_at) 이후 일정 시간 지나도 아무도
-- 확인 안 하면 notification-server가 재알림을 한 번 보내고, 그 시각을 여기 기록해 같은 알림을
-- 두 번 이상 에스컬레이션하지 않는다(escalated_at이 이미 있으면 대상에서 제외).
ALTER TABLE alerts ADD COLUMN escalated_at TIMESTAMP;
