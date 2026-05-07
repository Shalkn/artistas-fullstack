#!/usr/bin/env bash
# Gerado pelo Terraform (templatefile). Instala Tailscale se necessário,
# opcionalmente autentica com auth key, aplica funnel em background.
set -euo pipefail

AUTHKEY="$(printf '%s' '${auth_key_b64}' | base64 -d)"

install_tailscale() {
  if command -v tailscale >/dev/null 2>&1; then
    return 0
  fi
  echo "[tailscale-funnel] Instalando cliente Tailscale (install.sh)..."
  curl -fsSL https://tailscale.com/install.sh | sh
}

install_tailscale

if [[ -n "$AUTHKEY" ]]; then
  echo "[tailscale-funnel] tailscale up (auth key)..."
  sudo tailscale up --authkey="$AUTHKEY" --accept-dns=true
fi

echo "[tailscale-funnel] Limpando configuração anterior do Funnel (se existir)..."
sudo tailscale funnel reset || true

echo "[tailscale-funnel] Publicando http://127.0.0.1:${backend_port} na borda HTTPS ${https_port} (--bg)..."
sudo tailscale funnel --bg --https=${https_port} "http://127.0.0.1:${backend_port}"

echo "[tailscale-funnel] Concluído. Verifique: sudo tailscale funnel status"
