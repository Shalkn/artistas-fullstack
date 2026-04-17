/**
 * Espelha os DTOs JSON da API (`/api/v1`) usados pelo front. Campos seguem nomes camelCase da serialização Jackson.
 */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ArtistResponse {
  id: number
  name: string
  albumCount: number
}

export interface AlbumSummaryResponse {
  id: number
  title: string
  coverCount: number
}

export interface ArtistDetailResponse {
  id: number
  name: string
  albums: AlbumSummaryResponse[]
}

export interface AlbumDetailResponse {
  id: number
  artistId: number
  artistName: string
  title: string
  coverCount: number
  covers: CoverResponse[]
}

export interface CoverResponse {
  id: number
  contentType: string | null
  presignedUrl: string
  sortOrder: number
}

export interface AlbumCreatedEvent {
  albumId: number
  artistId: number
  artistName: string
  albumTitle: string
}
