/**
 * Camada de fachada da autenticação: login, logout e estado reativo da sessão.
 *
 * Tokens ficam em `sessionStorage` (chaves `artistas_access` / `artistas_refresh`) para
 * não persistir entre abas; o cliente deve usar a mesma origem que a API (proxy `/api`).
 */
import axios from 'axios'
import { BehaviorSubject } from 'rxjs'
import { httpClient } from '../api/httpClient'
import type { TokenResponse } from '../types/api'

const STORAGE_ACCESS = 'artistas_access'
const STORAGE_REFRESH = 'artistas_refresh'

export interface AuthState {
  accessToken: string
  refreshToken: string
}

/** Lê o par de tokens já persistido ao carregar o bundle (restaura sessão no F5). */
function loadInitial(): AuthState | null {
  const a = sessionStorage.getItem(STORAGE_ACCESS)
  const r = sessionStorage.getItem(STORAGE_REFRESH)
  if (a && r) return { accessToken: a, refreshToken: r }
  return null
}

/** Estado de sessão observável; componentes podem assinar via {@link useAuthState}. */
export const authState$ = new BehaviorSubject<AuthState | null>(loadInitial())

export const AuthFacade = {
  /** @returns `true` se existir access token no estado atual */
  isAuthenticated(): boolean {
    return !!authState$.value?.accessToken
  },

  /**
   * Autentica contra `POST /api/v1/auth/login` e persiste tokens.
   *
   * @throws propagado pelo Axios em credenciais inválidas ou erro de rede
   */
  async login(username: string, password: string): Promise<void> {
    const { data } = await httpClient.post<TokenResponse>('/api/v1/auth/login', {
      username,
      password,
    })
    const state: AuthState = {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
    }
    sessionStorage.setItem(STORAGE_ACCESS, state.accessToken)
    sessionStorage.setItem(STORAGE_REFRESH, state.refreshToken)
    authState$.next(state)
  },

  /** Remove tokens do storage e emite `null` no stream. */
  logout(): void {
    sessionStorage.removeItem(STORAGE_ACCESS)
    sessionStorage.removeItem(STORAGE_REFRESH)
    authState$.next(null)
  },
}

/**
 * Chama `POST ${window.location.origin}/api/v1/auth/refresh` com axios **fora** do `httpClient`
 * para não reentrar no interceptor de 401.
 *
 * @throws se não houver refresh token ou a API recusar
 */
export async function refreshAccessToken(): Promise<void> {
  const refresh = authState$.value?.refreshToken ?? sessionStorage.getItem(STORAGE_REFRESH)
  if (!refresh) throw new Error('Sem refresh token')
  const { data } = await axios.post<TokenResponse>(
    `${window.location.origin}/api/v1/auth/refresh`,
    { refreshToken: refresh }
  )
  const state: AuthState = {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
  }
  sessionStorage.setItem(STORAGE_ACCESS, state.accessToken)
  sessionStorage.setItem(STORAGE_REFRESH, state.refreshToken)
  authState$.next(state)
}
