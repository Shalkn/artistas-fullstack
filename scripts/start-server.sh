#!/usr/bin/env bash
set -euo pipefail

# Script de bootstrap pós-reboot para VM de deploy:
# - garante Docker ativo
# - garante Tailscale online
# - sobe stack com compose de produção
# - valida health da API
# - publica Tailscale Funnel para o frontend
#
# Variáveis suportadas (override por env):
#   DEPLOY_PATH=/home/renan/artistas-fullstack
#   COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"
#   FUNNEL_TARGET="http://127.0.0.1:80"
#   FUNNEL_HTTPS_PORT=443
#
# Exemplo:
#   DEPLOY_PATH=/home/renan/artistas-fullstack ./scripts/start-server.sh

DEPLOY_PATH="${DEPLOY_PATH:-/home/renan/artistas-fullstack}"
COMPOSE_FILES="${COMPOSE_FILES:--f docker-compose.yml -f docker-compose.prod.yml}"
FUNNEL_TARGET="${FUNNEL_TARGET:-http://127.0.0.1:80}"
FUNNEL_HTTPS_PORT="${FUNNEL_HTTPS_PORT:-443}"

compose() {
  # shellcheck disable=SC2086
  docker compose $COMPOSE_FILES --env-file .env "$@"
}

echo "==> [1/6] Checagens básicas"
df -h
echo
if sudo systemctl is-active --quiet docker; then
  echo "Docker: ativo"
else
  echo "Docker não está ativo. Iniciando..."
  sudo systemctl start docker
  sudo systemctl is-active --quiet docker
  echo "Docker: iniciado"
fi

echo
echo "==> [2/6] Tailscale"
if tailscale status >/dev/null 2>&1; then
  echo "Tailscale: ok"
else
  echo "Tailscale não respondeu. Tentando subir..."
  sudo tailscale up
fi
tailscale status

echo
echo "==> [3/6] Validar pasta de deploy"
if [ ! -d "$DEPLOY_PATH" ]; then
  echo "ERRO: DEPLOY_PATH não existe: $DEPLOY_PATH"
  exit 1
fi
if [ ! -f "$DEPLOY_PATH/.env" ]; then
  echo "ERRO: arquivo .env não encontrado em $DEPLOY_PATH"
  exit 1
fi

echo
echo "==> [4/6] Subir containers"
cd "$DEPLOY_PATH"
compose up -d --build
compose ps

echo
echo "==> [5/6] Healthcheck API"
compose exec -T api wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP
echo "API health: UP"

echo
echo "==> [6/6] Publicar Funnel"
if ! command -v tailscale >/dev/null 2>&1; then
  echo "ERRO: tailscale não encontrado na VM."
  exit 1
fi
if ! sudo -n true >/dev/null 2>&1; then
  echo "ERRO: sudo sem senha é necessário para aplicar tailscale funnel via script."
  exit 1
fi
sudo -n tailscale funnel reset || true
sudo -n tailscale funnel --https="$FUNNEL_HTTPS_PORT" "$FUNNEL_TARGET"
tailscale funnel status

echo
echo "Tudo OK: app no ar + funnel ativo."
