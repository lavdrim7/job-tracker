import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { listApplications } from './applications'
import { login } from './auth'
import { ApiError, getToken, request, setToken, setUnauthorizedHandler } from './client'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('API client', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    setToken(null)
    setUnauthorizedHandler(null)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  function lastCall() {
    const [url, init] = fetchMock.mock.calls.at(-1)!
    return { url, init: init!, headers: init!.headers as Record<string, string> }
  }

  it('sends the JWT as a Bearer token', async () => {
    fetchMock.mockResolvedValue(json([]))
    setToken('abc.def.ghi')

    await request('/api/applications')

    expect(lastCall().headers.Authorization).toBe('Bearer abc.def.ghi')
  })

  it('sends no Authorization header without a token', async () => {
    fetchMock.mockResolvedValue(json([]))

    await request('/api/applications')

    expect(lastCall().headers.Authorization).toBeUndefined()
  })

  it('sends JSON bodies with a content type', async () => {
    fetchMock.mockResolvedValue(json({ token: 't', tokenType: 'Bearer', expiresIn: 1 }))

    await login({ email: 'a@b.no', password: 'hemmelig-passord' })

    const { url, init, headers } = lastCall()
    expect(url).toBe('/api/auth/login')
    expect(init.method).toBe('POST')
    expect(headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(init.body as string)).toEqual({
      email: 'a@b.no',
      password: 'hemmelig-passord',
    })
  })

  it('clears the token and notifies the handler on 401', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    setToken('expired')
    fetchMock.mockResolvedValue(json({ status: 401, message: 'Ikke innlogget' }, 401))

    await expect(request('/api/applications')).rejects.toMatchObject({ status: 401 })

    expect(getToken()).toBeNull()
    expect(handler).toHaveBeenCalledOnce()
  })

  it('does not log out on 401 from login (wrong password)', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    setToken('still-valid')
    fetchMock.mockResolvedValue(
      json({ status: 401, message: 'Ugyldig e-post eller passord' }, 401),
    )

    await expect(login({ email: 'a@b.no', password: 'feil' })).rejects.toThrow(
      'Ugyldig e-post eller passord',
    )

    expect(handler).not.toHaveBeenCalled()
    expect(getToken()).toBe('still-valid')
  })

  it('exposes the backend message and field errors', async () => {
    fetchMock.mockResolvedValue(
      json(
        { status: 400, message: 'Ugyldig input', errors: ['company må fylles ut'] },
        400,
      ),
    )

    const error = await request('/api/applications', { method: 'POST', body: {} }).catch(
      (e: unknown) => e,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      message: 'Ugyldig input',
      errors: ['company må fylles ut'],
    })
  })

  it('wraps network failures in an ApiError', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(request('/api/applications')).rejects.toMatchObject({ status: 0 })
  })

  it('returns undefined for 204 responses', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(
      request('/api/applications/1', { method: 'DELETE' }),
    ).resolves.toBeUndefined()
  })

  it('builds the list query from status and sort', async () => {
    fetchMock.mockResolvedValue(json([]))

    await listApplications({ status: 'APPLIED', sort: 'deadline' })

    expect(lastCall().url).toBe('/api/applications?status=APPLIED&sort=deadline')
  })

  it('omits the query string when no filter is set', async () => {
    fetchMock.mockResolvedValue(json([]))

    await listApplications({ sort: 'created' })

    expect(lastCall().url).toBe('/api/applications')
  })
})
