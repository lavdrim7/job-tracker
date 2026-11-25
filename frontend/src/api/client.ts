/**
 * Minimal API client.
 *
 * The JWT is kept in a module-level variable (memory only), never in localStorage,
 * sessionStorage or a readable cookie. See README "Token storage" for the trade-off.
 */

let token: string | null = null
let unauthorizedHandler: (() => void) | null = null

export function setToken(value: string | null) {
  token = value
}

export function getToken() {
  return token
}

/** Called after a 401 on an authenticated request, once the token has been cleared. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

export class ApiError extends Error {
  readonly status: number
  readonly errors: string[]

  constructor(status: number, message: string, errors: string[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /** Login/register return 401/409 for ordinary user mistakes, which must not log anyone out. */
  auth?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true } = options
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth && token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Får ikke kontakt med serveren. Prøv igjen senere.')
  }

  if (!response.ok) {
    const problem = await readProblem(response)
    if (response.status === 401 && auth) {
      token = null
      unauthorizedHandler?.()
    }
    throw new ApiError(response.status, problem.message, problem.errors)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

async function readProblem(response: Response) {
  try {
    const data = (await response.json()) as { message?: string; errors?: string[] }
    return {
      message: data.message ?? `Forespørselen feilet (${response.status})`,
      errors: data.errors ?? [],
    }
  } catch {
    return { message: `Forespørselen feilet (${response.status})`, errors: [] }
  }
}
