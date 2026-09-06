-- FR-24/26 실시간 영상 뷰(CMD-002): CCTV/드론이 실제로 어디서 영상을 받아오는지 DB설계서 §3.2에
-- 필드가 없어 확인·재생이 불가능했다. 고정 위치 자산(CCTV/DRONE)에 한해 스트림 주소를 추가한다.
ALTER TABLE devices ADD COLUMN stream_url VARCHAR(500);
