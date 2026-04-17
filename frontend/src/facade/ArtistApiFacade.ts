/**
 * Fachada REST de artistas: encapsula `GET/POST/PUT` em `/api/v1/artists` e mantém cache opcional
 * da última página em {@link artistsListCache$}.
 */
import { BehaviorSubject } from 'rxjs'
import { httpClient } from '../api/httpClient'
import type {
  ArtistDetailResponse,
  ArtistResponse,
  PageResponse,
} from '../types/api'

/** Última resposta de listagem emitida (útil para estender a UI sem novo fetch imediato). */
export const artistsListCache$ = new BehaviorSubject<PageResponse<ArtistResponse> | null>(null)

export const ArtistApiFacade = {
  /**
   * Lista paginada com filtros opcionais.
   *
   * @param params.name filtro por nome; omitir para todos
   * @param params.sort ordenação alfabética
   * @param params.page base zero
   */
  async list(params: {
    name?: string
    sort?: 'asc' | 'desc'
    page?: number
    size?: number
  }): Promise<PageResponse<ArtistResponse>> {
    const { data } = await httpClient.get<PageResponse<ArtistResponse>>('/api/v1/artists', {
      params: {
        name: params.name || undefined,
        sort: params.sort ?? 'asc',
        page: params.page ?? 0,
        size: params.size ?? 10,
      },
    })
    artistsListCache$.next(data)
    return data
  },

  /** Detalhe com álbuns resumidos. */
  async getById(id: number): Promise<ArtistDetailResponse> {
    const { data } = await httpClient.get<ArtistDetailResponse>(`/api/v1/artists/${id}`)
    return data
  },

  /** Cria artista; contagem de álbuns vem zero na resposta. */
  async create(name: string): Promise<ArtistResponse> {
    const { data } = await httpClient.post<ArtistResponse>('/api/v1/artists', { name })
    return data
  },

  /** Atualiza nome e retorna contagem atual de álbuns. */
  async update(id: number, name: string): Promise<ArtistResponse> {
    const { data } = await httpClient.put<ArtistResponse>(`/api/v1/artists/${id}`, { name })
    return data
  },
}
