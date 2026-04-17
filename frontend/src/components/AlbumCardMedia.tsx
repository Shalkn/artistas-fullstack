import { useCallback, useEffect, useRef, useState } from 'react'
import { cn } from '../lib/cn'
import type { CoverResponse } from '../types/api'

/** Ordena capas pelo `sortOrder` definido no backend (exibição consistente no carrossel). */
function sortedCovers(covers: CoverResponse[]) {
  return [...covers].sort((a, b) => a.sortOrder - b.sortOrder)
}

type Props = {
  covers: CoverResponse[]
  albumTitle: string
  className?: string
}

/**
 * Carrossel em miniatura para cards de álbum (lista do artista).
 * Múltiplas capas: scroll-snap + indicadores; uma capa: imagem estática; zero: placeholder.
 */
export function AlbumCardMedia({ covers, albumTitle, className }: Props) {
  const ordered = sortedCovers(covers)
  const trackRef = useRef<HTMLDivElement>(null)
  const [active, setActive] = useState(0)
  const reduceMotion =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

  const scrollToIndex = useCallback(
    (index: number) => {
      const el = trackRef.current
      if (!el) return
      const clamped = Math.max(0, Math.min(index, ordered.length - 1))
      const slide = el.children[clamped] as HTMLElement | undefined
      if (!slide) return
      slide.scrollIntoView({
        behavior: reduceMotion ? 'auto' : 'smooth',
        block: 'nearest',
        inline: 'center',
      })
      setActive(clamped)
    },
    [ordered.length, reduceMotion]
  )

  useEffect(() => {
    const el = trackRef.current
    if (!el || ordered.length <= 1) return

    const onScroll = () => {
      const rect = el.getBoundingClientRect()
      const mid = rect.left + rect.width / 2
      let best = 0
      let bestDist = Infinity
      for (let i = 0; i < el.children.length; i++) {
        const child = el.children[i] as HTMLElement
        const c = child.getBoundingClientRect()
        const cMid = c.left + c.width / 2
        const d = Math.abs(cMid - mid)
        if (d < bestDist) {
          bestDist = d
          best = i
        }
      }
      setActive(best)
    }

    el.addEventListener('scroll', onScroll, { passive: true })
    return () => el.removeEventListener('scroll', onScroll)
  }, [ordered.length])

  return (
    <div
      className={cn(
        'group relative aspect-4/3 w-full max-h-44 overflow-hidden rounded-xl border border-white/10 bg-linear-to-br from-slate-800/90 to-slate-950/90 shadow-inner shadow-black/40 sm:max-w-[220px] sm:shrink-0',
        className
      )}
    >
      {ordered.length === 0 ? (
        <div className="flex h-full min-h-28 flex-col items-center justify-center gap-2 p-4 text-center">
          <div
            className="flex h-12 w-12 items-center justify-center rounded-full border border-white/10 bg-white/5 text-slate-500"
            aria-hidden
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" className="opacity-80">
              <path
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14M4 16h16"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>
          <span className="text-xs font-medium text-slate-500">Sem capas</span>
        </div>
      ) : (
        <>
          <div
            ref={trackRef}
            className="flex h-full snap-x snap-mandatory overflow-x-auto scroll-smooth [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
            tabIndex={0}
            role="region"
            aria-roledescription="carrossel"
            aria-label={`Miniaturas do álbum ${albumTitle}`}
          >
            {ordered.map((cover, index) => (
              <div key={cover.id} className="relative min-w-full snap-center">
                <img
                  src={cover.presignedUrl}
                  alt=""
                  className="h-full w-full object-cover"
                  loading={index === 0 ? 'eager' : 'lazy'}
                />
                <div
                  className="pointer-events-none absolute inset-0 bg-linear-to-t from-black/50 via-transparent to-black/20"
                  aria-hidden
                />
              </div>
            ))}
          </div>

          {ordered.length > 1 && (
            <>
              <button
                type="button"
                aria-label="Capa anterior"
                onClick={(e) => {
                  e.stopPropagation()
                  scrollToIndex(active - 1)
                }}
                disabled={active <= 0}
                className="absolute left-1.5 top-1/2 z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-white/20 bg-black/50 text-white opacity-0 shadow-md backdrop-blur-sm transition-all duration-200 hover:bg-black/70 focus:opacity-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:opacity-0 group-hover:opacity-100"
              >
                <span className="sr-only">Anterior</span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden>
                  <path
                    d="M15 18l-6-6 6-6"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                aria-label="Próxima capa"
                onClick={(e) => {
                  e.stopPropagation()
                  scrollToIndex(active + 1)
                }}
                disabled={active >= ordered.length - 1}
                className="absolute right-1.5 top-1/2 z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-white/20 bg-black/50 text-white opacity-0 shadow-md backdrop-blur-sm transition-all duration-200 hover:bg-black/70 focus:opacity-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:opacity-0 group-hover:opacity-100"
              >
                <span className="sr-only">Próxima</span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden>
                  <path
                    d="M9 18l6-6-6-6"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>
              <div
                className="pointer-events-none absolute bottom-2 left-0 right-0 flex justify-center gap-1.5"
                role="tablist"
                aria-label="Indicadores de capa"
              >
                {ordered.map((c, i) => (
                  <span
                    key={c.id}
                    className={cn(
                      'h-1.5 rounded-full transition-all duration-200',
                      i === active ? 'w-4 bg-accent' : 'w-1.5 bg-white/40'
                    )}
                  />
                ))}
              </div>
            </>
          )}
        </>
      )}
    </div>
  )
}
