import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Application } from '../api/types'
import { ApplicationTable } from './ApplicationTable'

function app(overrides: Partial<Application>): Application {
  return {
    id: 'id-1',
    company: 'Acme',
    position: 'Backend-utvikler',
    status: 'APPLIED',
    deadline: '2026-12-01',
    appliedDate: null,
    jobUrl: null,
    notes: null,
    createdAt: '2026-11-01T10:00:00Z',
    updatedAt: '2026-11-01T10:00:00Z',
    ...overrides,
  }
}

const applications = [
  app({ id: '1', company: 'Acme', status: 'APPLIED', deadline: '2026-12-01' }),
  app({
    id: '2',
    company: 'Globex',
    position: 'Frontend',
    status: 'OFFER',
    deadline: null,
  }),
]

function renderTable(props: Partial<React.ComponentProps<typeof ApplicationTable>> = {}) {
  const handlers = { onSortChange: vi.fn(), onEdit: vi.fn(), onDelete: vi.fn() }
  render(
    <ApplicationTable
      applications={applications}
      sort="created"
      {...handlers}
      {...props}
    />,
  )
  return handlers
}

describe('ApplicationTable', () => {
  it('renders one row per application with company, position, status and deadline', () => {
    renderTable()

    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3) // header + 2
    const acme = within(rows[1])
    expect(acme.getByRole('rowheader', { name: 'Acme' })).toBeInTheDocument()
    expect(acme.getByText('Backend-utvikler')).toBeInTheDocument()
    expect(acme.getByText('Søkt')).toHaveClass('badge-applied')
    expect(acme.getByText('01.12.2026')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Tilbud')).toHaveClass('badge-offer')
    expect(within(rows[2]).getByText('–')).toBeInTheDocument()
  })

  it('shows an empty state instead of an empty table', () => {
    renderTable({ applications: [] })

    expect(screen.getByText('Ingen søknader å vise.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('exposes sort state through aria-sort and toggles on click', async () => {
    const user = userEvent.setup()
    const { onSortChange } = renderTable()
    const header = screen.getByRole('columnheader', { name: /Frist/ })
    expect(header).toHaveAttribute('aria-sort', 'none')

    await user.click(within(header).getByRole('button'))

    expect(onSortChange).toHaveBeenCalledWith('deadline')
  })

  it('toggles back to default order when already sorted by deadline', async () => {
    const user = userEvent.setup()
    const { onSortChange } = renderTable({ sort: 'deadline' })
    const header = screen.getByRole('columnheader', { name: /Frist/ })
    expect(header).toHaveAttribute('aria-sort', 'ascending')

    await user.click(within(header).getByRole('button'))

    expect(onSortChange).toHaveBeenCalledWith('created')
  })

  it('gives edit and delete buttons a unique accessible name per row', async () => {
    const user = userEvent.setup()
    const { onEdit, onDelete } = renderTable()

    await user.click(screen.getByRole('button', { name: 'Rediger Globex' }))
    await user.click(screen.getByRole('button', { name: 'Slett Acme' }))

    expect(onEdit).toHaveBeenCalledWith(applications[1])
    expect(onDelete).toHaveBeenCalledWith(applications[0])
  })

  it('reaches row actions with the keyboard', async () => {
    const user = userEvent.setup()
    const { onEdit } = renderTable()

    await user.tab() // sort button
    await user.tab() // first Rediger
    expect(screen.getByRole('button', { name: 'Rediger Acme' })).toHaveFocus()
    await user.keyboard('{Enter}')

    expect(onEdit).toHaveBeenCalledWith(applications[0])
  })
})
