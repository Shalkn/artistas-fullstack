import { Navigate } from 'react-router-dom'
import { AuthFacade } from '../facade/AuthFacade'

/**
 * Guarda de rota: renderiza `children` apenas se houver sessão (`AuthFacade.isAuthenticated()`); senão redireciona a `/login`.
 */
export function PrivateRoute({ children }: { children: React.ReactNode }) {
  if (!AuthFacade.isAuthenticated()) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}
