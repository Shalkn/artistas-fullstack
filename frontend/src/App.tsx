/**
 * Raiz da SPA: roteamento, code-splitting por página (`React.lazy`), toasts globais e layout autenticado.
 */
import { lazy, Suspense } from 'react'
import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { AuthFacade } from './facade/AuthFacade'
import { useAlbumWebSocket } from './hooks/useAlbumWebSocket'
import { PrivateRoute } from './routes/PrivateRoute'
import { cn } from './lib/cn'

const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const ArtistsListPage = lazy(() =>
  import('./pages/ArtistsListPage').then((m) => ({ default: m.ArtistsListPage }))
)
const ArtistDetailPage = lazy(() =>
  import('./pages/ArtistDetailPage').then((m) => ({ default: m.ArtistDetailPage }))
)
const ArtistFormPage = lazy(() =>
  import('./pages/ArtistFormPage').then((m) => ({ default: m.ArtistFormPage }))
)
const AlbumDetailPage = lazy(() =>
  import('./pages/AlbumDetailPage').then((m) => ({ default: m.AlbumDetailPage }))
)
const AlbumFormPage = lazy(() =>
  import('./pages/AlbumFormPage').then((m) => ({ default: m.AlbumFormPage }))
)

/**
 * Layout das rotas autenticadas: cabeçalho, área principal e assinatura WebSocket de novos álbuns.
 * O logout usa `window.location.href` para garantir estado limpo após sair.
 */
function Shell({ children }: { children: React.ReactNode }) {
  useAlbumWebSocket(true)
  return (
    <div className="min-h-screen flex flex-col">
      <header className="sticky top-0 z-20 border-b border-white/10 bg-surface/85 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4">
          <Link
            to="/"
            className={cn(
              'font-display text-xl tracking-wide text-white transition-colors duration-200',
              'hover:text-accent focus:outline-none focus-visible:ring-2 focus-visible:ring-accent rounded-md cursor-pointer'
            )}
          >
            Artistas
          </Link>
          <nav className="flex items-center gap-3">
            <Link
              to="/"
              className="hidden text-sm text-slate-300 transition-colors hover:text-white sm:inline cursor-pointer"
            >
              Catálogo
            </Link>
            <button
              type="button"
              onClick={() => {
                AuthFacade.logout()
                window.location.href = '/login'
              }}
              className="rounded-lg border border-white/15 bg-white/5 px-4 py-2 text-sm font-medium text-slate-200 transition-all duration-200 hover:border-white/25 hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer"
            >
              Sair
            </button>
          </nav>
        </div>
      </header>
      <main className="flex-1">{children}</main>
      <footer className="border-t border-white/10 py-6 text-center text-xs text-slate-500">
        Catálogo de artistas e álbuns
      </footer>
    </div>
  )
}

/** Fallback do `Suspense` enquanto chunks de página são carregados. */
function PageLoader() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <div className="flex flex-col items-center gap-3">
        <div
          className="h-9 w-9 animate-spin rounded-full border-2 border-accent/30 border-t-accent"
          aria-hidden
        />
        <p className="text-sm text-slate-400">Carregando…</p>
      </div>
    </div>
  )
}

/** Configura `BrowserRouter`, rotas públicas (`/login`) e protegidas com `PrivateRoute` + `Shell`. */
export default function App() {
  return (
    <BrowserRouter>
      <Toaster richColors position="top-right" closeButton />
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <Shell>
                  <ArtistsListPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route
            path="/artists/new"
            element={
              <PrivateRoute>
                <Shell>
                  <ArtistFormPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route
            path="/artists/:id/edit"
            element={
              <PrivateRoute>
                <Shell>
                  <ArtistFormPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route
            path="/artists/:id"
            element={
              <PrivateRoute>
                <Shell>
                  <ArtistDetailPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route
            path="/albums/new"
            element={
              <PrivateRoute>
                <Shell>
                  <AlbumFormPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route
            path="/albums/:id/edit"
            element={
              <PrivateRoute>
                <Shell>
                  <AlbumFormPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route
            path="/albums/:id"
            element={
              <PrivateRoute>
                <Shell>
                  <AlbumDetailPage />
                </Shell>
              </PrivateRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
