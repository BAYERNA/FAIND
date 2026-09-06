import { apiRequest } from './client'
import type { CameraResponse, DeviceResponse } from '../types'

// CMD-002 드론 정찰 카드: 배정된 드론 기기 상세(배터리·상태)
export function getDevice(deviceId: string): Promise<DeviceResponse> {
  return apiRequest<DeviceResponse>(`/api/v1/devices/${deviceId}`)
}

// CMD-002 라이브 카메라 선택 드롭다운(FR-24/26)
export function getCameras(): Promise<CameraResponse[]> {
  return apiRequest<CameraResponse[]>('/api/v1/devices/cameras')
}

// ai-server MJPEG 중계 엔드포인트 — dev server의 vite.config.ts가 /ai-stream을 ai-server로 프록시한다.
// 순수 영상 중계이며 감지·판단은 하지 않는다 (Phase 1의 danger 신호는 별도 채널).
export function buildLiveStreamUrl(streamUrl: string): string {
  return `/ai-stream/mjpeg?stream_url=${encodeURIComponent(streamUrl)}`
}
