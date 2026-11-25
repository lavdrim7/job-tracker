import { useEffect, useId, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import { STATUSES, STATUS_LABELS } from '../api/types'
import type { Application, ApplicationInput, ApplicationStatus } from '../api/types'
import { validateApplication } from '../lib/validation'
import type {
  ApplicationField,
  ApplicationFormValues,
  FieldErrors,
} from '../lib/validation'
import { Field, FieldShell } from './Field'

interface ApplicationFormProps {
  /** Present when editing; absent when creating. */
  application?: Application
  onSubmit: (input: ApplicationInput) => Promise<void>
  onCancel: () => void
}

const FIELD_ORDER: ApplicationField[] = ['company', 'position', 'jobUrl']

function toValues(application?: Application): ApplicationFormValues {
  return {
    company: application?.company ?? '',
    position: application?.position ?? '',
    status: application?.status ?? 'WISHLIST',
    deadline: application?.deadline ?? '',
    appliedDate: application?.appliedDate ?? '',
    jobUrl: application?.jobUrl ?? '',
    notes: application?.notes ?? '',
  }
}

export function ApplicationForm({
  application,
  onSubmit,
  onCancel,
}: ApplicationFormProps) {
  const [values, setValues] = useState(() => toValues(application))
  const [errors, setErrors] = useState<FieldErrors<ApplicationField>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const formRef = useRef<HTMLFormElement>(null)
  const statusId = useId()
  const notesId = useId()

  // Move focus into the form when it opens so keyboard and screen-reader users land in it.
  useEffect(() => headingRef.current?.focus(), [])

  function set<K extends ApplicationField>(key: K, value: ApplicationFormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    const found = validateApplication(values)
    setErrors(found)
    const firstInvalid = FIELD_ORDER.find((field) => found[field])
    if (firstInvalid) {
      formRef.current?.querySelector<HTMLElement>(`[name="${firstInvalid}"]`)?.focus()
      return
    }

    setSubmitting(true)
    try {
      await onSubmit({
        company: values.company.trim(),
        position: values.position.trim(),
        status: values.status,
        deadline: values.deadline || null,
        appliedDate: values.appliedDate || null,
        jobUrl: values.jobUrl.trim() || null,
        notes: values.notes.trim() || null,
      })
    } catch (error) {
      setFormError(
        error instanceof ApiError
          ? [error.message, ...error.errors].join(': ')
          : 'Noe gikk galt. Prøv igjen.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  const errorList = Object.values(errors)
  return (
    <form
      ref={formRef}
      onSubmit={handleSubmit}
      noValidate
      aria-labelledby="app-form-title"
      className="card"
    >
      <h2 id="app-form-title" ref={headingRef} tabIndex={-1}>
        {application ? 'Rediger søknad' : 'Ny søknad'}
      </h2>
      {(formError || errorList.length > 0) && (
        <div role="alert" className="form-error">
          <p>{formError ?? 'Rett opp feilene under:'}</p>
          {!formError && (
            <ul>
              {errorList.map((message) => (
                <li key={message}>{message}</li>
              ))}
            </ul>
          )}
        </div>
      )}
      <Field
        label="Selskap"
        name="company"
        required
        autoComplete="organization"
        value={values.company}
        error={errors.company}
        onChange={(e) => set('company', e.target.value)}
      />
      <Field
        label="Stilling"
        name="position"
        required
        autoComplete="organization-title"
        value={values.position}
        error={errors.position}
        onChange={(e) => set('position', e.target.value)}
      />
      <FieldShell id={statusId} label="Status">
        <select
          id={statusId}
          name="status"
          value={values.status}
          onChange={(e) => set('status', e.target.value as ApplicationStatus)}
        >
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {STATUS_LABELS[status]}
            </option>
          ))}
        </select>
      </FieldShell>
      <Field
        label="Frist"
        name="deadline"
        type="date"
        value={values.deadline}
        onChange={(e) => set('deadline', e.target.value)}
      />
      <Field
        label="Søknadsdato"
        name="appliedDate"
        type="date"
        value={values.appliedDate}
        onChange={(e) => set('appliedDate', e.target.value)}
      />
      <Field
        label="Lenke til stillingen"
        name="jobUrl"
        type="url"
        autoComplete="off"
        value={values.jobUrl}
        error={errors.jobUrl}
        onChange={(e) => set('jobUrl', e.target.value)}
      />
      <FieldShell id={notesId} label="Notater">
        <textarea
          id={notesId}
          name="notes"
          rows={4}
          value={values.notes}
          onChange={(e) => set('notes', e.target.value)}
        />
      </FieldShell>
      <div className="actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Lagrer …' : 'Lagre'}
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Avbryt
        </button>
      </div>
    </form>
  )
}
