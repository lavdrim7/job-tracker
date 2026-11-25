import { useId } from 'react'
import type { ComponentProps, ReactNode } from 'react'

interface FieldProps extends ComponentProps<'input'> {
  label: string
  error?: string
  hint?: string
}

/** Label + input + inline error, wired together with htmlFor and aria-describedby. */
export function Field({ label, error, hint, ...inputProps }: FieldProps) {
  const id = useId()
  return (
    <FieldShell id={id} label={label} error={error} hint={hint}>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(id, error, hint)}
        {...inputProps}
      />
    </FieldShell>
  )
}

export function FieldShell({
  id,
  label,
  error,
  hint,
  children,
}: {
  id: string
  label: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {hint && (
        <span id={`${id}-hint`} className="hint">
          {hint}
        </span>
      )}
      {children}
      {error && (
        <span id={`${id}-error`} className="field-error">
          {error}
        </span>
      )}
    </div>
  )
}

export function describedBy(id: string, error?: string, hint?: string) {
  const ids = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean)
  return ids.length > 0 ? ids.join(' ') : undefined
}
