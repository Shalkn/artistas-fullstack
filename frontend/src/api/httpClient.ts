/**
 * Cliente HTTP compartilhado pela aplicação.
 *
 * - **Request:** injeta `Authorization: Bearer` a partir do valor atual de `authState$`.
 * - **Response:** em `401`/`403`, tenta `refreshAccessToken()` uma vez e repete a chamada;
 *   falha no refresh redireciona para `/login`.
 *
 * Uso: importe `httpClient` nas fachadas; não use para `POST /auth/refresh` (use axios puro em
 * `refreshAccessToken` para evitar loop no interceptor).
 */
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { AuthFacade, authState$, refreshAccessToken } from '../facade/AuthFacade'

export const httpClient = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
})

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const access = authState$.value?.accessToken
  if (access && config.headers) {
    config.headers.Authorization = `Bearer ${access}`
  }
  return config
})

httpClient.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    if (original?.url?.includes('/auth/refresh')) {
      return Promise.reject(error)
    }
    const status = error.response?.status
    // 401 = não autenticado; 403 ainda aparece em alguns proxies/CORS antigos — tenta refresh e evita loop
    if ((status === 401 || status === 403) && original && !original._retry) {
      original._retry = true
      try {
        await refreshAccessToken()
        const access = authState$.value?.accessToken
        if (access && original.headers) {
          original.headers.Authorization = `Bearer ${access}`
        }
        return httpClient(original)
      } catch {
        AuthFacade.logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)
