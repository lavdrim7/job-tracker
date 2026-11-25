import { request } from './client'
import type {
  Application,
  ApplicationInput,
  ApplicationSort,
  ApplicationStatus,
} from './types'

export interface ListParams {
  status?: ApplicationStatus
  sort?: ApplicationSort
}

export function listApplications({ status, sort }: ListParams = {}) {
  const query = new URLSearchParams()
  if (status) query.set('status', status)
  if (sort === 'deadline') query.set('sort', 'deadline')
  const suffix = query.size > 0 ? `?${query}` : ''
  return request<Application[]>(`/api/applications${suffix}`)
}

export function createApplication(input: ApplicationInput) {
  return request<Application>('/api/applications', { method: 'POST', body: input })
}

export function updateApplication(id: string, input: ApplicationInput) {
  return request<Application>(`/api/applications/${id}`, { method: 'PUT', body: input })
}

export function deleteApplication(id: string) {
  return request<void>(`/api/applications/${id}`, { method: 'DELETE' })
}
