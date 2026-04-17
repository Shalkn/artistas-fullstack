import { useCallback, useEffect, useRef, useState } from 'react'
import { cn } from '../lib/cn'
import type { CoverResponse } from '../types/api'

/** Replica a ordenação por `sortOrder` usada na página de detalhe. */
function sortedCovers(covers: CoverResponse[]) {
  return [...covers].sort((a, b) => a.sortOrder - b.sortOrder)
}

function ChevronLeft({ className }: { className?: string }) {
  return (
    <svg className={className} width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M15 18l-6-6 6-6"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function ChevronRight({ className }: { className?: string }) {
  return (
    <svg className={className} width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M9 18l6-6-6-6"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

type Props = {
  covers: CoverResponse[]
  albumTitle: string
  className?: string
}

/**
 * Carrossel de capas com scroll-snap, setas, indicadores e teclado (←/→).
 * Respeita prefers-reduced-motion via CSS global no container de scroll.
 */
export function CoverCarousel({ covers, albumTitle, className }: Props) {
  const ordered = sortedCovers(covers)
  const [active, setActive] = useState(0)
  const trackRef = useRef<HTMLDivElement>(null)
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
    if (!el || ordered.length === 0) return

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

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') {
        e.preventDefault()
        scrollToIndex(active - 1)
      }
      if (e.key === 'ArrowRight') {
        e.preventDefault()
        scrollToIndex(active + 1)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [active, scrollToIndex])

  if (ordered.length === 0) return null

  return (
    <div className={cn('space-y-4', className)}>
      <div className="relative group rounded-2xl border border-white/10 bg-surface-elevated/80 overflow-hidden shadow-2xl shadow-primary/10">
        <div
          ref={trackRef}
          className="flex overflow-x-auto snap-x snap-mandatory gap-0 scroll-smooth [scrollbar-width:thin]"
          style={{ scrollbarColor: 'rgba(255,255,255,0.2) transparent' }}
          tabIndex={0}
          role="region"
          aria-roledescription="carrossel"
          aria-label={`Capas do álbum ${albumTitle}`}
        >
          {ordered.map((cover, index) => (
            <div
              key={cover.id}
              className="min-w-full snap-center flex items-center justify-center p-4 sm:p-8"
            >
              <img
                src={cover.presignedUrl}
                alt={`Capa ${index + 1} de ${ordered.length} — ${albumTitle}`}
                className="max-h-[min(55vh,420px)] w-auto max-w-full object-contain rounded-lg shadow-lg"
                loading={index === 0 ? 'eager' : 'lazy'}
              />
            </div>
          ))}
        </div>

        {ordered.length > 1 && (
          <>
            <button
              type="button"
              aria-label="Capa anterior"
              onClick={() => scrollToIndex(active - 1)}
              disabled={active <= 0}
              className="absolute left-2 top-1/2 -translate-y-1/2 flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-surface/90 text-white shadow-lg backdrop-blur-sm transition-opacity duration-200 hover:bg-white/10 hover:border-white/25 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:opacity-30 cursor-pointer"
            >
              <ChevronLeft className="h-6 w-6" />
            </button>
            <button
              type="button"
              aria-label="Próxima capa"
              onClick={() => scrollToIndex(active + 1)}
              disabled={active >= ordered.length - 1}
              className="absolute right-2 top-1/2 -translate-y-1/2 flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-surface/90 text-white shadow-lg backdrop-blur-sm transition-opacity duration-200 hover:bg-white/10 hover:border-white/25 focus:outline-none focus-visible:ring-2 focus-visible:ring-accent disabled:opacity-30 cursor-pointer"
            >
              <ChevronRight className="h-6 w-6" />
            </button>
          </>
        )}
      </div>

      {ordered.length > 1 && (
        <div className="flex flex-wrap items-center justify-center gap-2" role="tablist" aria-label="Selecionar capa">
          {ordered.map((cover, index) => (
            <button
              key={cover.id}
              type="button"
              role="tab"
              aria-selected={index === active}
              aria-label={`Ir para capa ${index + 1}`}
              onClick={() => scrollToIndex(index)}
              className={cn(
                'relative h-14 w-14 shrink-0 overflow-hidden rounded-lg border-2 transition-all duration-200 cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-accent',
                index === active
                  ? 'border-accent ring-2 ring-accent/40 scale-105'
                  : 'border-white/10 opacity-70 hover:opacity-100 hover:border-white/25'
              )}
            >
              <img
                src={cover.presignedUrl}
                alt=""
                className="h-full w-full object-cover"
                loading="lazy"
              />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
