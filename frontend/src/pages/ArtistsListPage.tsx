import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArtistApiFacade } from '../facade/ArtistApiFacade'
import { cn } from '../lib/cn'
import type { ArtistResponse, PageResponse } from '../types/api'

/** Listagem paginada de artistas com busca por nome, ordenação e link para criação. */
export function ArtistsListPage() {
  const [data, setData] = useState<PageResponse<ArtistResponse> | null>(null)
  const [name, setName] = useState('')
  const [sort, setSort] = useState<'asc' | 'desc'>('asc')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await ArtistApiFacade.list({
        name: name || undefined,
        sort,
        page,
        size: 8,
      })
      setData(res)
    } finally {
      setLoading(false)
    }
  }, [name, sort, page])

  useEffect(() => {
    void load()
  }, [load])

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div className="mb-10 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-2">
          <h1 className="font-display text-3xl text-white sm:text-4xl">Catálogo</h1>
          <p className="max-w-xl text-sm leading-relaxed text-slate-400">
            Busque por nome e ordene alfabeticamente. Cartões com hover para navegação rápida.
          </p>
        </div>
        <Link
          to="/artists/new"
          className={cn(
            'inline-flex items-center justify-center rounded-xl bg-accent px-5 py-3 text-sm font-semibold text-surface shadow-lg shadow-accent/25',
            'transition-all duration-200 hover:bg-accent-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
          )}
        >
          Novo artista
        </Link>
      </div>

      <div className="mb-8 flex flex-wrap items-center gap-3">
        <input
          placeholder="Buscar por nome…"
          className={cn(
            'min-w-[220px] flex-1 rounded-xl border border-white/10 bg-surface-elevated/80 px-4 py-2.5 text-sm text-white placeholder:text-slate-500 sm:max-w-md',
            'transition-colors duration-200 focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/25'
          )}
          value={name}
          onChange={(e) => {
            setPage(0)
            setName(e.target.value)
          }}
          aria-label="Buscar artista por nome"
        />
        <select
          className={cn(
            'rounded-xl border border-white/10 bg-surface-elevated/80 px-4 py-2.5 text-sm text-white',
            'cursor-pointer transition-colors focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/25'
          )}
          value={sort}
          onChange={(e) => {
            setPage(0)
            setSort(e.target.value as 'asc' | 'desc')
          }}
          aria-label="Ordenação"
        >
          <option value="asc">Nome A–Z</option>
          <option value="desc">Nome Z–A</option>
        </select>
      </div>

      {loading && (
        <div className="flex justify-center py-16">
          <div className="flex flex-col items-center gap-3">
            <div
              className="h-9 w-9 animate-spin rounded-full border-2 border-accent/30 border-t-accent"
              aria-hidden
            />
            <p className="text-sm text-slate-400">Carregando artistas…</p>
          </div>
        </div>
      )}

      {!loading && data && (
        <>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-2">
            {data.content.map((a) => (
              <li key={a.id}>
                <Link
                  to={`/artists/${a.id}`}
                  className={cn(
                    'group block rounded-2xl border border-white/10 bg-surface-card p-6 transition-all duration-200',
                    'hover:border-primary/40 hover:bg-white/[0.06] hover:shadow-lg hover:shadow-primary/10',
                    'focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
                  )}
                >
                  <h2 className="text-lg font-semibold text-white transition-colors group-hover:text-accent">
                    {a.name}
                  </h2>
                  <p className="mt-2 text-sm text-slate-400">
                    {a.albumCount} {a.albumCount === 1 ? 'álbum' : 'álbuns'}
                  </p>
                </Link>
              </li>
            ))}
          </ul>

          {data.content.length === 0 && (
            <p className="rounded-2xl border border-dashed border-white/15 py-16 text-center text-slate-500">
              Nenhum artista encontrado.
            </p>
          )}

          <nav
            className="mt-10 flex flex-wrap items-center justify-center gap-3 border-t border-white/10 pt-8"
            aria-label="Paginação"
          >
            <button
              type="button"
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className={cn(
                'rounded-xl border border-white/15 px-4 py-2 text-sm font-medium transition-all duration-200',
                'hover:border-white/30 hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-35',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
              )}
            >
              Anterior
            </button>
            <span className="text-sm text-slate-400">
              Página {data.page + 1} de {Math.max(1, data.totalPages)} · {data.totalElements} artistas
            </span>
            <button
              type="button"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
              className={cn(
                'rounded-xl border border-white/15 px-4 py-2 text-sm font-medium transition-all duration-200',
                'hover:border-white/30 hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-35',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
              )}
            >
              Próxima
            </button>
          </nav>
        </>
      )}
    </div>
  )
}
