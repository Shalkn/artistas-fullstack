import { useEffect, useState } from 'react'
import { authState$, type AuthState } from '../facade/AuthFacade'

/**
 * Inscreve o componente no `BehaviorSubject` `authState$` e devolve o estado atual de autenticação.
 * Desinscreve ao desmontar.
 *
 * @returns `null` se não houver sessão; caso contrário access + refresh tokens
 */
export function useAuthState(): AuthState | null {
  const [state, setState] = useState<AuthState | null>(authState$.value)
  useEffect(() => {
    const sub = authState$.subscribe(setState)
    return () => sub.unsubscribe()
  }, [])
  return state
}
