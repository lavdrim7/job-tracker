import type { Application, ApplicationSort } from '../api/types'
import { formatDate } from '../lib/dates'
import { StatusBadge } from './StatusBadge'

interface ApplicationTableProps {
  applications: Application[]
  sort: ApplicationSort
  onSortChange: (sort: ApplicationSort) => void
  onEdit: (application: Application) => void
  onDelete: (application: Application) => void
}

export function ApplicationTable({
  applications,
  sort,
  onSortChange,
  onEdit,
  onDelete,
}: ApplicationTableProps) {
  if (applications.length === 0) {
    return <p>Ingen søknader å vise.</p>
  }

  const byDeadline = sort === 'deadline'
  return (
    <table>
      <caption className="visually-hidden">Jobbsøknader</caption>
      <thead>
        <tr>
          <th scope="col">Selskap</th>
          <th scope="col">Stilling</th>
          <th scope="col">Status</th>
          <th scope="col" aria-sort={byDeadline ? 'ascending' : 'none'}>
            <button
              type="button"
              className="link-button"
              onClick={() => onSortChange(byDeadline ? 'created' : 'deadline')}
            >
              Frist {byDeadline ? '▲' : ''}
              <span className="visually-hidden">
                {byDeadline
                  ? ', sortert stigende. Aktiver for nyeste først'
                  : ', aktiver for å sortere'}
              </span>
            </button>
          </th>
          <th scope="col">Handlinger</th>
        </tr>
      </thead>
      <tbody>
        {applications.map((application) => (
          <tr key={application.id}>
            <th scope="row">{application.company}</th>
            <td>{application.position}</td>
            <td>
              <StatusBadge status={application.status} />
            </td>
            <td>{formatDate(application.deadline)}</td>
            <td className="actions">
              <button
                type="button"
                aria-label={`Rediger ${application.company}`}
                onClick={() => onEdit(application)}
              >
                Rediger
              </button>
              <button
                type="button"
                className="danger"
                aria-label={`Slett ${application.company}`}
                onClick={() => onDelete(application)}
              >
                Slett
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
