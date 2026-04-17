import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { AlbumApiFacade } from '../facade/AlbumApiFacade'
import { cn } from '../lib/cn'

/**
 * Criação ou edição de álbum. Query opcional `?artistId=` pré-seleciona o artista em novo álbum.
 */
export function AlbumFormPage() {
  const { id } = useParams<{ id: string }>()
  const [search] = useSearchParams()
  const nav = useNavigate()
  const isEdit = Boolean(id)
  const defaultArtist = Number(search.get('artistId') || 0)

  const [artistId, setArtistId] = useState(defaultArtist)
  const [title, setTitle] = useState('')
  const [loading, setLoading] = useState(isEdit)

  useEffect(() => {
    if (!isEdit || !id) {
      setLoading(false)
      return
    }
    void (async () => {
      try {
        const al = await AlbumApiFacade.getById(Number(id))
        setArtistId(al.artistId)
        setTitle(al.title)
      } catch {
        toast.error('Álbum não encontrado')
        nav('/')
      } finally {
        setLoading(false)
      }
    })()
  }, [id, isEdit, nav])

  /** Valida artista + título e chama create/update na API. */
  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!artistId || !title.trim()) {
      toast.error('Preencha artista e título')
      return
    }
    try {
      if (isEdit && id) {
        await AlbumApiFacade.update(Number(id), artistId, title.trim())
        toast.success('Álbum atualizado')
        nav(`/albums/${id}`)
      } else {
        const created = await AlbumApiFacade.create(artistId, title.trim())
        toast.success('Álbum criado')
        nav(`/albums/${created.id}`)
      }
    } catch {
      toast.error('Erro ao salvar')
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center px-4">
        <div className="h-9 w-9 animate-spin rounded-full border-2 border-accent/30 border-t-accent" aria-hidden />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-10">
      <Link
        to={artistId ? `/artists/${artistId}` : '/'}
        className="inline-flex items-center gap-2 text-sm text-accent transition-colors hover:text-emerald-300 cursor-pointer"
      >
        ← Voltar
      </Link>
      <h1 className="font-display mt-4 text-3xl text-white">{isEdit ? 'Editar álbum' : 'Novo álbum'}</h1>
      <form onSubmit={onSubmit} className="mt-8 space-y-6">
        <div>
          <label htmlFor="album-artist" className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
            ID do artista
          </label>
          <input
            id="album-artist"
            type="number"
            className={cn(
              'w-full rounded-xl border border-white/10 bg-surface-elevated/80 px-4 py-3 text-white',
              'focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/25'
            )}
            value={artistId || ''}
            onChange={(e) => setArtistId(Number(e.target.value))}
            required
            min={1}
          />
        </div>
        <div>
          <label htmlFor="album-title" className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Título
          </label>
          <input
            id="album-title"
            className={cn(
              'w-full rounded-xl border border-white/10 bg-surface-elevated/80 px-4 py-3 text-white',
              'focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/25'
            )}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>
        <button
          type="submit"
          className={cn(
            'w-full rounded-xl bg-accent py-3 text-sm font-semibold text-surface shadow-lg shadow-accent/20',
            'transition-all duration-200 hover:bg-accent-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-accent cursor-pointer'
          )}
        >
          Salvar
        </button>
      </form>
    </div>
  )
}
