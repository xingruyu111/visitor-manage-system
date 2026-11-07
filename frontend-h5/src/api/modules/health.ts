import { http } from '@/api/request'

export interface HealthInfo {
  status: string
  service: string
  time: string
}

export function getHealth(): Promise<HealthInfo> {
  return http.get<HealthInfo>('/health')
}
