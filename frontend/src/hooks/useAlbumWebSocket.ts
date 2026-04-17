import { useEffect } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { toast } from 'sonner'
import type { AlbumCreatedEvent } from '../types/api'
import { useAuthState } from './useAuthState'

/**
 * Conecta ao endpoint SockJS da mesma origem (`/ws`), assina o tópico STOMP `/topic/albums` e
 * exibe toast (Sonner) ao receber JSON compatível com `AlbumCreatedEvent`.
 *
 * Só ativa a conexão quando `enabled` é verdadeiro **e** existe access token (usuário logado).
 *
 * @param enabled normalmente `true` no shell autenticado; use `false` para desligar em rotas públicas
 */
export function useAlbumWebSocket(enabled: boolean) {
  const auth = useAuthState()

  useEffect(() => {
    if (!enabled || !auth?.accessToken) return

    const client = new Client({
      webSocketFactory: () => new SockJS(`${window.location.origin}/ws`) as unknown as WebSocket,
      connectHeaders: {},
      reconnectDelay: 4000,
      onConnect: () => {
        client.subscribe('/topic/albums', (message) => {
          try {
            const ev = JSON.parse(message.body) as AlbumCreatedEvent
            toast.info('Novo álbum cadastrado', {
              description: `${ev.artistName} — ${ev.albumTitle}`,
            })
          } catch {
            /* ignore */
          }
        })
      },
    })

    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [enabled, auth?.accessToken])
}
