export const STATUSES = ['WISHLIST', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'] as const
export type ApplicationStatus = (typeof STATUSES)[number]

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  WISHLIST: 'Ønskeliste',
  APPLIED: 'Søkt',
  INTERVIEW: 'Intervju',
  OFFER: 'Tilbud',
  REJECTED: 'Avslått',
}

export interface Application {
  id: string
  company: string
  position: string
  status: ApplicationStatus
  deadline: string | null
  appliedDate: string | null
  jobUrl: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

/** Body for both POST and PUT (PUT replaces the whole resource). */
export interface ApplicationInput {
  company: string
  position: string
  status: ApplicationStatus
  deadline: string | null
  appliedDate: string | null
  jobUrl: string | null
  notes: string | null
}

export type ApplicationSort = 'created' | 'deadline'

export interface Credentials {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
}
