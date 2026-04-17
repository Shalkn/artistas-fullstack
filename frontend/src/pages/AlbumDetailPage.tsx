import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { CoverCarousel } from '../components/CoverCarousel'
import { AlbumApiFacade } from '../facade/AlbumApiFacade'
import { cn } from '../lib/cn'
import type { AlbumDetailResponse } from '../types/api'

/** Exibe metadados do álbum e carrossel de capas (`CoverCarousel`) com URLs pré-assinadas. */
export function AlbumDetailPage() {
  const { id } = useParams<{ id: string }>()
  const nav = useNavigate()
  const [album, setAlbum] = useState<AlbumDetailResponse | null>(null)

  useEffect(() => {
    if (!id) return
    void (async () => {
      try {
        setAlbum(await AlbumApiFacade.getById(Number(id)))
      } catch {
        toast.error('Álbum não encontrado')
        nav('/')
      }
    })()
  }, [id, nav])

  if (!album) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center px-4">
        <div className="flex flex-col items-center gap-3">
          <div
            className="h-9 w-9 animate-spin rounded-full border-2 border-accent/30 border-t-accent"
            aria-hidden
          />
          <p className="text-sm text-slate-400">Carregando álbum…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-2">
          <Link
            to={`/artists/${album.artistId}`}
            className="inline-flex items-center gap-2 text-sm text-accent transition-colors hover:text-emerald-300 cursor-pointer"
          >
            <span aria-hidden className="text-lg leading-none">
              ←
            </span>
            {album.artistName}
          </Link>
          <h1 className="font-display text-3xl text-white sm:text-4xl">{album.title}</h1>
          <p className="text-sm text-slate-400">
            {album.coverCount === 0
              ? 'Nenhuma capa enviada'
              : `${album.coverCount} ${album.coverCount === 1 ? 'imagem' : 'imagens'}`}
          </p>
        </div>
        <Link
          to={`/albums/${album.id}/edit`}
          className={cn(
            'inline-flex shrink-0 justify-center rounded-xl border border-white/15 px-4 py-2.5 text-sm font-medium',
            'bg-white/5 text-slate-100 transition-all duration-200 hover:border-white/25 hover:bg-white/10',
            'focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
          )}
        >
          Editar álbum
        </Link>
      </div>

      <section className="space-y-4">
        <h2 className="text-lg font-semibold tracking-tight text-white">Capas do álbum</h2>
        {album.covers.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-white/15 bg-surface-elevated/50 px-6 py-14 text-center">
            <p className="text-slate-400">Sem imagens ainda.</p>
            <p className="mt-2 text-sm text-slate-500">
              Envie capas pela página do artista ou pelo upload no cartão do álbum.
            </p>
          </div>
        ) : (
          <CoverCarousel covers={album.covers} albumTitle={album.title} />
        )}
      </section>
    </div>
  )
}
