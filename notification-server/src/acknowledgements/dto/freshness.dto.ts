// CMD-002 annot#3: "12분 전 확인, 갱신 필요" 표시를 위한 집계 응답.
export class FreshnessDto {
  alertId: string;
  acknowledgedUserIds: string[];
  lastAcknowledgedAt: string | null;
  staleMinutesThreshold: number;
  isStale: boolean;
}
