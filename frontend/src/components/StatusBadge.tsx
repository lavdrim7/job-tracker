import { STATUS_LABELS } from '../api/types'
import type { ApplicationStatus } from '../api/types'

/** The status is always spelled out, so colour is never the only signal. */
export function StatusBadge({ status }: { status: ApplicationStatus }) {
  return (
    <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
  )
}
