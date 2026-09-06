import { apiRequest } from './client'
import type { DeviceResponse, DeviceType, Page } from '../types'

export interface DeviceListParams {
  keyword?: string
  deviceType?: string
  page?: number
  size?: number
}

export function listDevices(params: DeviceListParams): Promise<Page<DeviceResponse>> {
  return apiRequest<Page<DeviceResponse>>('/api/v1/devices', {
    query: { keyword: params.keyword, deviceType: params.deviceType, page: params.page, size: params.size },
  })
}

export interface DeviceRegisterInput {
  deviceType: DeviceType
  serialNo: string
  connectionType?: string
  currentUserId?: string
  latitude?: number
  longitude?: number
  batteryLevel?: number
}

export function registerDevice(input: DeviceRegisterInput): Promise<DeviceResponse> {
  return apiRequest<DeviceResponse>('/api/v1/devices', { method: 'POST', body: input })
}

export function remapDevice(deviceId: string, userId: string): Promise<DeviceResponse> {
  return apiRequest<DeviceResponse>(`/api/v1/devices/${deviceId}/mapping`, { method: 'PATCH', body: { userId } })
}

export function relocateDevice(deviceId: string, latitude: number, longitude: number): Promise<DeviceResponse> {
  return apiRequest<DeviceResponse>(`/api/v1/devices/${deviceId}/location`, {
    method: 'PATCH',
    body: { latitude, longitude },
  })
}
