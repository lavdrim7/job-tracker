import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthForm } from '../components/AuthForm'

export function LoginPage() {
  const { isAuthenticated, sessionExpired, login } = useAuth()
  if (isAuthenticated) return <Navigate to="/" replace />

  return (
    <main>
      <AuthForm
        title="Logg inn"
        submitLabel="Logg inn"
        checkPasswordLength={false}
        passwordAutoComplete="current-password"
        notice={sessionExpired ? 'Økten din har utløpt. Logg inn på nytt.' : undefined}
        onSubmit={(email, password) => login({ email, password })}
      />
      <p>
        Har du ikke konto? <Link to="/register">Registrer deg</Link>
      </p>
    </main>
  )
}
