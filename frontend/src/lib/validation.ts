import type { ApplicationStatus } from '../api/types'

export type FieldErrors<T extends string> = Partial<Record<T, string>>

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateCredentials(
  email: string,
  password: string,
  { checkPasswordLength }: { checkPasswordLength: boolean },
): FieldErrors<'email' | 'password'> {
  const errors: FieldErrors<'email' | 'password'> = {}
  if (!email.trim()) errors.email = 'E-post må fylles ut'
  else if (!EMAIL.test(email.trim())) errors.email = 'Skriv inn en gyldig e-postadresse'

  if (!password) errors.password = 'Passord må fylles ut'
  else if (checkPasswordLength && (password.length < 10 || password.length > 72)) {
    errors.password = 'Passordet må være mellom 10 og 72 tegn'
  }
  return errors
}

export interface ApplicationFormValues {
  company: string
  position: string
  status: ApplicationStatus
  deadline: string
  appliedDate: string
  jobUrl: string
  notes: string
}

export type ApplicationField = keyof ApplicationFormValues

export function validateApplication(
  values: ApplicationFormValues,
): FieldErrors<ApplicationField> {
  const errors: FieldErrors<ApplicationField> = {}
  if (!values.company.trim()) errors.company = 'Selskap må fylles ut'
  else if (values.company.length > 255) errors.company = 'Selskap kan ha maks 255 tegn'

  if (!values.position.trim()) errors.position = 'Stilling må fylles ut'
  else if (values.position.length > 255) errors.position = 'Stilling kan ha maks 255 tegn'

  if (values.jobUrl.trim()) {
    if (values.jobUrl.length > 2048) errors.jobUrl = 'Lenken kan ha maks 2048 tegn'
    else if (!isHttpUrl(values.jobUrl.trim())) {
      errors.jobUrl = 'Lenken må starte med http:// eller https://'
    }
  }
  return errors
}

function isHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}
