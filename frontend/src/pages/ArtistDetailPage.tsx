import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ArtistApiFacade } from '../facade/ArtistApiFacade'
import { AlbumApiFacade } from '../facade/AlbumApiFacade'
import { AlbumCardMedia } from '../components/AlbumCardMedia'
import { cn } from '../lib/cn'
import type { AlbumDetailResponse, ArtistDetailResponse } from '../types/api'

/**
 * Carrega detalhe do artista e a primeira página grande de álbuns com capas pré-assinadas
 * (para alimentar os cartões com miniaturas).
 */
async function loadArtistAndAlbums(artistId: number): Promise<{
  artist: ArtistDetailResponse
  albums: AlbumDetailResponse[]
}> {
  const [artist, albumsPage] = await Promise.all([
    ArtistApiFacade.getById(artistId),
    AlbumApiFacade.search({
      artistId,
      page: 0,
      size: 100,
      includePresignedCovers: true,
    }),
  ])
  return { artist, albums: albumsPage.content }
}

/** Detalhe do artista, lista de álbuns com mídia e ações de edição/navegação. */
export function ArtistDetailPage() {
  const { id } = useParams<{ id: string }>()
  const nav = useNavigate()
  const [artist, setArtist] = useState<ArtistDetailResponse | null>(null)
  const [albumCards, setAlbumCards] = useState<AlbumDetailResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [title, setTitle] = useState('')
  const [uploading, setUploading] = useState(false)

  const refreshData = useCallback(async (artistId: number) => {
    const { artist: nextArtist, albums } = await loadArtistAndAlbums(artistId)
    setArtist(nextArtist)
    setAlbumCards(albums)
  }, [])

  useEffect(() => {
    if (!id) return
    void (async () => {
      setLoading(true)
      try {
        await refreshData(Number(id))
      } catch {
        toast.error('Artista não encontrado')
        nav('/')
      } finally {
        setLoading(false)
      }
    })()
  }, [id, nav, refreshData])

  async function addAlbum(e: React.FormEvent) {
    e.preventDefault()
    if (!artist || !title.trim()) return
    try {
      await AlbumApiFacade.create(artist.id, title.trim())
      setTitle('')
      await refreshData(artist.id)
      toast.success('Álbum criado')
    } catch {
      toast.error('Erro ao criar álbum')
    }
  }

  async function onUpload(albumId: number, files: FileList | null) {
    if (!files?.length) return
    setUploading(true)
    try {
      await AlbumApiFacade.uploadCovers(albumId, files)
      if (artist) await refreshData(artist.id)
      toast.success('Capas enviadas')
    } catch {
      toast.error('Falha no upload')
    } finally {
      setUploading(false)
    }
  }

  if (loading || !artist) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center px-4">
        <div className="flex flex-col items-center gap-3">
          <div
            className="h-9 w-9 animate-spin rounded-full border-2 border-accent/30 border-t-accent"
            aria-hidden
          />
          <p className="text-sm text-slate-400">Carregando artista…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <div className="mb-10 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-sm text-accent transition-colors hover:text-emerald-300 cursor-pointer"
          >
            <span aria-hidden>←</span> Catálogo
          </Link>
          <h1 className="font-display mt-3 text-3xl text-white sm:text-4xl">{artist.name}</h1>
        </div>
        <Link
          to={`/artists/${artist.id}/edit`}
          className={cn(
            'inline-flex shrink-0 justify-center rounded-xl border border-white/15 px-4 py-2.5 text-sm font-medium',
            'bg-white/5 text-slate-100 transition-all duration-200 hover:border-white/25 hover:bg-white/10',
            'focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
          )}
        >
          Editar artista
        </Link>
      </div>

      <section
        className={cn(
          'mb-10 rounded-2xl border border-white/10 bg-surface-elevated/60 p-6 shadow-lg shadow-primary/5',
          'backdrop-blur-sm'
        )}
      >
        <h2 className="text-lg font-semibold text-white">Novo álbum</h2>
        <p className="mt-1 text-sm text-slate-400">Crie um álbum e envie capas em seguida.</p>
        <form onSubmit={addAlbum} className="mt-4 flex flex-col gap-3 sm:flex-row">
          <input
            className={cn(
              'min-w-0 flex-1 rounded-xl border border-white/10 bg-surface px-4 py-2.5 text-sm text-white placeholder:text-slate-500',
              'focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/25'
            )}
            placeholder="Título do álbum"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            aria-label="Título do novo álbum"
          />
          <button
            type="submit"
            className={cn(
              'rounded-xl bg-accent px-6 py-2.5 text-sm font-semibold text-surface shadow-md shadow-accent/20',
              'transition-all duration-200 hover:bg-accent-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
            )}
          >
            Adicionar
          </button>
        </form>
      </section>

      <section>
        <h2 className="mb-4 text-lg font-semibold text-white">Álbuns</h2>
        {albumCards.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-white/15 py-12 text-center text-slate-500">
            Nenhum álbum cadastrado ainda.
          </p>
        ) : (
          <ul className="space-y-4">
            {albumCards.map((al) => (
              <li
                key={al.id}
                className={cn(
                  'overflow-hidden rounded-2xl border border-white/10 bg-surface-card transition-colors duration-200',
                  'hover:border-primary/30'
                )}
              >
                <div className="flex flex-col gap-0 sm:flex-row sm:items-stretch">
                  <AlbumCardMedia covers={al.covers} albumTitle={al.title} className="sm:rounded-l-2xl sm:rounded-r-none sm:border-r sm:border-white/10" />
                  <div className="flex min-w-0 flex-1 flex-col justify-between gap-4 p-5">
                    <div className="min-w-0">
                      <Link
                        to={`/albums/${al.id}`}
                        className="text-lg font-semibold text-accent transition-colors hover:text-emerald-300 cursor-pointer"
                      >
                        {al.title}
                      </Link>
                      <p className="mt-1 text-sm text-slate-500">
                        {al.coverCount} {al.coverCount === 1 ? 'capa' : 'capas'}
                      </p>
                    </div>
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-end">
                      <label
                        className={cn(
                          'inline-flex cursor-pointer items-center justify-center rounded-xl border border-dashed border-white/20 px-4 py-2.5 text-sm text-slate-300',
                          'transition-colors hover:border-accent/50 hover:bg-white/5',
                          uploading && 'pointer-events-none opacity-50'
                        )}
                      >
                        <input
                          type="file"
                          multiple
                          accept="image/*"
                          disabled={uploading}
                          className="sr-only"
                          onChange={(e) => void onUpload(al.id, e.target.files)}
                        />
                        Enviar capas
                      </label>
                      <Link
                        to={`/albums/${al.id}/edit`}
                        className="text-center text-sm text-slate-400 underline-offset-4 transition-colors hover:text-white cursor-pointer"
                      >
                        Editar álbum
                      </Link>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
