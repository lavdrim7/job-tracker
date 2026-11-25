import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import { validateCredentials } from '../lib/validation'
import type { FieldErrors } from '../lib/validation'
import { Field } from './Field'

interface AuthFormProps {
  title: string
  submitLabel: string
  /** Register enforces the backend's 10-72 character password rule; login does not. */
  checkPasswordLength: boolean
  passwordAutoComplete: 'current-password' | 'new-password'
  notice?: string
  onSubmit: (email: string, password: string) => Promise<void>
}

export function AuthForm({
  title,
  submitLabel,
  checkPasswordLength,
  passwordAutoComplete,
  notice,
  onSubmit,
}: AuthFormProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<FieldErrors<'email' | 'password'>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const emailRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    const found = validateCredentials(email, password, { checkPasswordLength })
    setErrors(found)
    if (found.email) return emailRef.current?.focus()
    if (found.password) return passwordRef.current?.focus()

    setSubmitting(true)
    try {
      await onSubmit(email.trim(), password)
    } catch (error) {
      setFormError(
        error instanceof ApiError ? describe(error) : 'Noe gikk galt. Prøv igjen.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      aria-labelledby="auth-title"
      className="card"
    >
      <h1 id="auth-title">{title}</h1>
      {notice && <p role="status">{notice}</p>}
      {formError && (
        <p role="alert" className="form-error">
          {formError}
        </p>
      )}
      <Field
        ref={emailRef}
        label="E-post"
        type="email"
        name="email"
        autoComplete="email"
        value={email}
        error={errors.email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Field
        label="Passord"
        type="password"
        name="password"
        autoComplete={passwordAutoComplete}
        value={password}
        error={errors.password}
        hint={checkPasswordLength ? 'Minst 10 tegn' : undefined}
        onChange={(e) => setPassword(e.target.value)}
        ref={passwordRef}
      />
      <button type="submit" disabled={submitting}>
        {submitting ? 'Vent …' : submitLabel}
      </button>
    </form>
  )
}

function describe(error: ApiError) {
  if (error.errors.length > 0) return `${error.message}: ${error.errors.join(', ')}`
  return error.message
}
