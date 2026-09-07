-- IncidentNumberGenerator가 인메모리 AtomicInteger였을 때, 애플리케이션 재기동 시 시퀀스가
-- 0으로 리셋되어 기존 incidents.incident_number(UNIQUE)와 충돌하는 결함이 있었다.
-- DB 시퀀스로 옮겨 재기동·다중 인스턴스 양쪽에서 항상 유일한 값을 보장한다.
CREATE SEQUENCE incident_number_seq START WITH 1 INCREMENT BY 1;
