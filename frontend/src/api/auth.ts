import { request } from './client'
import type { Credentials, LoginResponse } from './types'

export function register(credentials: Credentials) {
  return request<{ id: string; email: string }>('/api/auth/register', {
    method: 'POST',
    body: credentials,
    auth: false,
  })
}

export function login(credentials: Credentials) {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: credentials,
    auth: false,
  })
}
