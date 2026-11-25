import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/auth'
import { setToken, setUnauthorizedHandler } from '../api/client'
import type { Credentials } from '../api/types'

interface AuthState {
  isAuthenticated: boolean
  /** True after a 401 forced a logout; the login page explains why. */
  sessionExpired: boolean
  login: (credentials: Credentials) => Promise<void>
  register: (credentials: Credentials) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setAuthenticated] = useState(false)
  const [sessionExpired, setSessionExpired] = useState(false)

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setAuthenticated(false)
      setSessionExpired(true)
    })
    return () => setUnauthorizedHandler(null)
  }, [])

  const login = useCallback(async (credentials: Credentials) => {
    const { token } = await authApi.login(credentials)
    setToken(token)
    setSessionExpired(false)
    setAuthenticated(true)
  }, [])

  const register = useCallback(async (credentials: Credentials) => {
    await authApi.register(credentials)
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    setSessionExpired(false)
    setAuthenticated(false)
  }, [])

  const value = useMemo(
    () => ({ isAuthenticated, sessionExpired, login, register, logout }),
    [isAuthenticated, sessionExpired, login, register, logout],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth må brukes inni AuthProvider')
  return context
}
