import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthFacade } from '../facade/AuthFacade'
import { cn } from '../lib/cn'

/** Formulário de credenciais; redireciona ao catálogo após `AuthFacade.login` bem-sucedido. */
export function LoginPage() {
  const nav = useNavigate()

  useEffect(() => {
    if (AuthFacade.isAuthenticated()) {
      nav('/', { replace: true })
    }
  }, [nav])
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  /** Envia login à API; erros genéricos são mostrados na própria página. */
  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await AuthFacade.login(username, password)
      nav('/', { replace: true })
    } catch {
      setError('Falha no login. Verifique usuário e senha.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 py-12">
      <div className="mb-10 text-center">
        <h1 className="font-display text-4xl tracking-wide text-white sm:text-5xl">Artistas</h1>
        <p className="mt-2 text-sm text-slate-400">Acesse o catálogo de artistas e álbuns</p>
      </div>
      <form
        onSubmit={onSubmit}
        className={cn(
          'w-full max-w-md space-y-5 rounded-2xl border border-white/10 bg-surface-elevated/90 p-8 shadow-2xl shadow-primary/20',
          'backdrop-blur-sm'
        )}
      >
        <div className="space-y-1">
          <h2 className="text-lg font-semibold text-white">Entrar</h2>
          <p className="text-sm text-slate-400">Credenciais padrão no README do projeto.</p>
        </div>
        {error && (
          <p className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
            {error}
          </p>
        )}
        <div>
          <label htmlFor="login-user" className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Usuário
          </label>
          <input
            id="login-user"
            className={cn(
              'w-full rounded-xl border border-white/10 bg-surface px-3 py-2.5 text-sm text-white placeholder:text-slate-600',
              'transition-colors duration-200 focus:border-primary/60 focus:outline-none focus:ring-2 focus:ring-primary/30'
            )}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />
        </div>
        <div>
          <label htmlFor="login-pass" className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Senha
          </label>
          <input
            id="login-pass"
            type="password"
            className={cn(
              'w-full rounded-xl border border-white/10 bg-surface px-3 py-2.5 text-sm text-white placeholder:text-slate-600',
              'transition-colors duration-200 focus:border-primary/60 focus:outline-none focus:ring-2 focus:ring-primary/30'
            )}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className={cn(
            'w-full rounded-xl bg-accent py-3 text-sm font-semibold text-surface shadow-lg shadow-accent/25',
            'transition-all duration-200 hover:bg-accent-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-surface',
            'disabled:opacity-50 cursor-pointer'
          )}
        >
          {loading ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
