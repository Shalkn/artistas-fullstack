/**
 * Fachada REST de álbuns: busca paginada, CRUD e upload multipart de capas (`files` no FormData).
 */
import { httpClient } from '../api/httpClient'
import type { AlbumDetailResponse, PageResponse } from '../types/api'

export const AlbumApiFacade = {
  /**
   * Busca com filtros; use `includePresignedCovers: true` quando precisar de URLs nas capas (mais custoso no backend).
   */
  async search(params: {
    artistId?: number
    title?: string
    page?: number
    size?: number
    includePresignedCovers?: boolean
  }): Promise<PageResponse<AlbumDetailResponse>> {
    const { data } = await httpClient.get<PageResponse<AlbumDetailResponse>>('/api/v1/albums', {
      params: {
        artistId: params.artistId,
        title: params.title,
        page: params.page ?? 0,
        size: params.size ?? 10,
        includePresignedCovers: params.includePresignedCovers ?? false,
      },
    })
    return data
  },

  /** Detalhe com lista de capas e URLs pré-assinadas. */
  async getById(id: number): Promise<AlbumDetailResponse> {
    const { data } = await httpClient.get<AlbumDetailResponse>(`/api/v1/albums/${id}`)
    return data
  },

  /** Cria álbum; o backend publica evento WebSocket em `/topic/albums`. */
  async create(artistId: number, title: string): Promise<AlbumDetailResponse> {
    const { data } = await httpClient.post<AlbumDetailResponse>('/api/v1/albums', {
      artistId,
      title,
    })
    return data
  },

  /** Permite trocar o artista dono e o título. */
  async update(id: number, artistId: number, title: string): Promise<AlbumDetailResponse> {
    const { data } = await httpClient.put<AlbumDetailResponse>(`/api/v1/albums/${id}`, {
      artistId,
      title,
    })
    return data
  },

  /**
   * Envia um ou mais arquivos no campo `files` (multipart). Não retorna corpo tipado; recarregue o álbum se precisar das URLs.
   */
  async uploadCovers(albumId: number, files: FileList | File[]): Promise<void> {
    const form = new FormData()
    const arr = Array.from(files)
    arr.forEach((f) => form.append('files', f))
    await httpClient.post(`/api/v1/albums/${albumId}/covers`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
