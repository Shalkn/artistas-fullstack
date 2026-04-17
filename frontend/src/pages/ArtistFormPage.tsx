import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ArtistApiFacade } from '../facade/ArtistApiFacade'
import { cn } from '../lib/cn'

/** Criação (`/artists/new`) ou edição (`/artists/:id/edit`) de artista. */
export function ArtistFormPage() {
  const { id } = useParams<{ id: string }>()
  const nav = useNavigate()
  const isEdit = Boolean(id)
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(isEdit)

  useEffect(() => {
    if (!isEdit || !id) return
    void (async () => {
      try {
        const d = await ArtistApiFacade.getById(Number(id))
        setName(d.name)
      } catch {
        toast.error('Não foi possível carregar o artista')
        nav('/')
      } finally {
        setLoading(false)
      }
    })()
  }, [id, isEdit, nav])

  /** Persiste create ou update e navega para o detalhe do artista. */
  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    try {
      if (isEdit && id) {
        await ArtistApiFacade.update(Number(id), name.trim())
        toast.success('Artista atualizado')
        nav(`/artists/${id}`)
      } else {
        const created = await ArtistApiFacade.create(name.trim())
        toast.success('Artista criado')
        nav(`/artists/${created.id}`)
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
        to={isEdit && id ? `/artists/${id}` : '/'}
        className="inline-flex items-center gap-2 text-sm text-accent transition-colors hover:text-emerald-300 cursor-pointer"
      >
        ← Voltar
      </Link>
      <h1 className="font-display mt-4 text-3xl text-white">
        {isEdit ? 'Editar artista' : 'Novo artista'}
      </h1>
      <form onSubmit={onSubmit} className="mt-8 space-y-6">
        <div>
          <label htmlFor="artist-name" className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
            Nome
          </label>
          <input
            id="artist-name"
            className={cn(
              'w-full rounded-xl border border-white/10 bg-surface-elevated/80 px-4 py-3 text-white',
              'focus:border-primary/50 focus:outline-none focus:ring-2 focus:ring-primary/25'
            )}
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
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
