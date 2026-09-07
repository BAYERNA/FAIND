# fire_yolov8.pt 출처·라이선스

이 파일(`fire_yolov8.pt`)은 화재/연기로 파인튜닝된 YOLOv8n 가중치로, FR-24(CCTV 자동 화재감지)와
FR-26(드론 정찰)이 실제로 동작하도록 만드는 데 필요하다.

- **원본**: [Nocluee100/Fire-and-Smoke-Detection-yolov8-v1](https://github.com/Nocluee100/Fire-and-Smoke-Detection-yolov8-v1)
  (`weights/best.pt`)
- **코드 라이선스**: MIT License
- **학습 데이터셋**: Roboflow "FireSmokeNEWdataset" (업로더: catargiuconstantin2), **CC BY 4.0**
- **클래스**: `{0: fire, 1: other, 2: smoke}` — `other`는 밝은 조명 등을 화재로 오인하지 않도록 별도로
  학습된 클래스다.

두 라이선스 모두 상업적 이용을 포함해 자유로운 사용·수정·배포를 허용하되, 저작자 표시를 요구한다.
이 NOTICE 파일이 그 저작자 표시 역할을 겸한다 — 별도 배포판을 만들 때도 이 파일은 함께 유지할 것.

직접 파인튜닝을 추가로 진행한 모델로 교체하려면, 같은 경로(`ai-server/models/fire_yolov8.pt`)에
덮어쓰기만 하면 된다(코드 변경 불필요). 원본이 AGPL-3.0 저장소(luminous0219/fire-and-smoke-detection-yolov8)
에서 파생된 가중치가 아니라는 점은 확인됐으나, 향후 다른 소스로 교체할 경우 라이선스를 다시 확인할 것.
