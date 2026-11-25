import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getToken, setToken } from '../api/client'
import { AuthProvider } from '../auth/AuthContext'
import { LoginPage } from './LoginPage'

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('LoginPage', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    setToken(null)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('has labelled fields reachable by label text', () => {
    renderLogin()

    expect(screen.getByLabelText('E-post')).toHaveAttribute('type', 'email')
    expect(screen.getByLabelText('Passord')).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Logg inn' })).toBeInTheDocument()
  })

  it('shows field errors, focuses the first invalid field and does not call the API', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByRole('button', { name: 'Logg inn' }))

    const email = screen.getByLabelText('E-post')
    expect(screen.getByText('E-post må fylles ut')).toBeInTheDocument()
    expect(screen.getByText('Passord må fylles ut')).toBeInTheDocument()
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveAccessibleDescription('E-post må fylles ut')
    expect(email).toHaveFocus()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('rejects a malformed e-mail address', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('E-post'), 'ikke-en-epost')
    await user.type(screen.getByLabelText('Passord'), 'hemmelig-passord')
    await user.click(screen.getByRole('button', { name: 'Logg inn' }))

    expect(screen.getByText('Skriv inn en gyldig e-postadresse')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('logs in and keeps the token in memory only', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      json({ token: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 }),
    )
    renderLogin()

    await user.type(screen.getByLabelText('E-post'), 'ola@example.com')
    await user.type(screen.getByLabelText('Passord'), 'hemmelig-passord')
    await user.click(screen.getByRole('button', { name: 'Logg inn' }))

    await waitFor(() => expect(getToken()).toBe('jwt-token'))
    expect(JSON.parse(fetchMock.mock.calls[0][1]!.body as string)).toEqual({
      email: 'ola@example.com',
      password: 'hemmelig-passord',
    })
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('announces a server error in an alert', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      json({ status: 401, message: 'Ugyldig e-post eller passord' }, 401),
    )
    renderLogin()

    await user.type(screen.getByLabelText('E-post'), 'ola@example.com')
    await user.type(screen.getByLabelText('Passord'), 'feil-passord')
    await user.click(screen.getByRole('button', { name: 'Logg inn' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ugyldig e-post eller passord',
    )
    expect(getToken()).toBeNull()
  })

  it('can be submitted with the keyboard', async () => {
    const user = userEvent.setup()
    fetchMock.mockResolvedValue(
      json({ token: 't', tokenType: 'Bearer', expiresIn: 3600 }),
    )
    renderLogin()

    await user.type(screen.getByLabelText('E-post'), 'ola@example.com')
    await user.tab()
    expect(screen.getByLabelText('Passord')).toHaveFocus()
    await user.keyboard('hemmelig-passord{Enter}')

    await waitFor(() => expect(fetchMock).toHaveBeenCalledOnce())
  })
})
