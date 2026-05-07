#!/usr/bin/env bash
# Bootstrap idempotente — Ubuntu Server (apt).
# Docker apenas via repositório oficial (download.docker.com), sem misturar com docker.io do Ubuntu.
# Executado na VM com sudo pelo Terraform remote-exec.

set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

echo "[bootstrap] Atualizando índice de pacotes..."
apt-get update -qq

echo "[bootstrap] Instalando pacotes base..."
apt-get install -y --no-install-recommends \
  ca-certificates \
  curl \
  git \
  gnupg \
  wget

echo "[bootstrap] Configurando repositório oficial Docker (Engine + Compose v2)..."
# docker.io (Ubuntu) conflita com containerd.io do Docker Engine — remover só se estiver instalado.
if dpkg-query -W -f='${Status}' docker.io 2>/dev/null | grep -q "install ok installed"; then
  echo "[bootstrap] Removendo pacote docker.io (distro) para evitar conflito com Docker Engine oficial..."
  apt-get remove -y docker.io
fi

install -m 0755 -d /etc/apt/keyrings
if [[ ! -f /etc/apt/keyrings/docker.asc ]]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
fi
chmod a+r /etc/apt/keyrings/docker.asc

# VERSION_CODENAME: jammy, noble, etc.
# shellcheck source=/dev/null
. /etc/os-release
CODENAME="${VERSION_CODENAME:-${UBUNTU_CODENAME:-}}"
if [[ -z "${CODENAME}" ]]; then
  echo "[bootstrap] ERRO: não foi possível determinar o codename do Ubuntu (/etc/os-release)." >&2
  exit 1
fi

ARCH="$(dpkg --print-architecture)"
echo "[bootstrap] Escrevendo /etc/apt/sources.list.d/docker.list (codename=${CODENAME}, arch=${ARCH})..."
# Linha deve começar com "deb" — nunca prefixar com texto de log ou o apt falha (Type '...' is not known).
echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${CODENAME} stable" \
  >/etc/apt/sources.list.d/docker.list

echo "[bootstrap] Instalando Docker Engine e plugins (mesma origem: Docker Inc.)..."
apt-get update -qq
apt-get install -y --no-install-recommends \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin

echo "[bootstrap] Habilitando serviço Docker..."
systemctl enable --now docker

BOOT_USER="${SUDO_USER:-${USER:-root}}"
if id -u "${BOOT_USER}" >/dev/null 2>&1 && [[ "${BOOT_USER}" != "root" ]]; then
  echo "[bootstrap] Adicionando '${BOOT_USER}' ao grupo docker..."
  usermod -aG docker "${BOOT_USER}" || true
fi

echo "[bootstrap] Versões instaladas:"
docker --version
docker compose version

echo "[bootstrap] Concluído."
