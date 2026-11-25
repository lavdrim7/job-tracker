import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthForm } from '../components/AuthForm'

export function RegisterPage() {
  const { register, login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(email: string, password: string) {
    await register({ email, password })
    await login({ email, password })
    navigate('/', { replace: true })
  }

  return (
    <main>
      <AuthForm
        title="Registrer deg"
        submitLabel="Opprett konto"
        checkPasswordLength
        passwordAutoComplete="new-password"
        onSubmit={handleSubmit}
      />
      <p>
        Har du allerede konto? <Link to="/login">Logg inn</Link>
      </p>
    </main>
  )
}
