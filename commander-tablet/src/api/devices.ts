import { apiRequest } from './client'
import type { DeviceResponse } from '../types'

// CMD-002 드론 정찰 카드: 배정된 드론 기기 상세(배터리·상태)
export function getDevice(deviceId: string): Promise<DeviceResponse> {
  return apiRequest<DeviceResponse>(`/api/v1/devices/${deviceId}`)
}
