import { useCallback, useEffect, useId, useState } from 'react'
import {
  createApplication,
  deleteApplication,
  listApplications,
  updateApplication,
} from '../api/applications'
import { ApiError } from '../api/client'
import { STATUSES, STATUS_LABELS } from '../api/types'
import type {
  Application,
  ApplicationInput,
  ApplicationSort,
  ApplicationStatus,
} from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ApplicationForm } from '../components/ApplicationForm'
import { ApplicationTable } from '../components/ApplicationTable'

type FormState =
  { mode: 'closed' } | { mode: 'create' } | { mode: 'edit'; application: Application }

export function ApplicationsPage() {
  const { logout } = useAuth()
  const [applications, setApplications] = useState<Application[]>([])
  const [status, setStatus] = useState<ApplicationStatus | ''>('')
  const [sort, setSort] = useState<ApplicationSort>('created')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>({ mode: 'closed' })
  const filterId = useId()

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setApplications(await listApplications({ status: status || undefined, sort }))
    } catch (e) {
      // A 401 is handled globally (logout + redirect), so only show other failures.
      if (!(e instanceof ApiError && e.status === 401)) {
        setError(e instanceof ApiError ? e.message : 'Kunne ikke hente søknader.')
      }
    } finally {
      setLoading(false)
    }
  }, [status, sort])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch on mount and when filters change
    void load()
  }, [load])

  async function handleSave(input: ApplicationInput) {
    if (form.mode === 'edit') await updateApplication(form.application.id, input)
    else await createApplication(input)
    setForm({ mode: 'closed' })
    await load()
  }

  async function handleDelete(application: Application) {
    if (!window.confirm(`Slette søknaden til ${application.company}?`)) return
    try {
      await deleteApplication(application.id)
      await load()
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) {
        setError(e instanceof ApiError ? e.message : 'Kunne ikke slette søknaden.')
      }
    }
  }

  return (
    <main>
      <header className="page-header">
        <h1>Søknader</h1>
        <button type="button" className="secondary" onClick={logout}>
          Logg ut
        </button>
      </header>

      {form.mode === 'closed' ? (
        <button type="button" onClick={() => setForm({ mode: 'create' })}>
          Ny søknad
        </button>
      ) : (
        <ApplicationForm
          key={form.mode === 'edit' ? form.application.id : 'new'}
          application={form.mode === 'edit' ? form.application : undefined}
          onSubmit={handleSave}
          onCancel={() => setForm({ mode: 'closed' })}
        />
      )}

      <div className="field filter">
        <label htmlFor={filterId}>Filtrer på status</label>
        <select
          id={filterId}
          value={status}
          onChange={(e) => setStatus(e.target.value as ApplicationStatus | '')}
        >
          <option value="">Alle</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <p role="alert" className="form-error">
          {error}
        </p>
      )}
      <div aria-live="polite" aria-busy={loading}>
        {loading && applications.length === 0 ? (
          <p>Laster …</p>
        ) : (
          <ApplicationTable
            applications={applications}
            sort={sort}
            onSortChange={setSort}
            onEdit={(application) => setForm({ mode: 'edit', application })}
            onDelete={handleDelete}
          />
        )}
      </div>
    </main>
  )
}
