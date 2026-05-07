variable "ssh_host" {
  description = "IP ou hostname da VM já existente (bootstrap remoto por SSH)."
  type        = string
}

variable "ssh_user" {
  description = "Usuário SSH com permissão sudo na VM."
  type        = string
}

variable "ssh_port" {
  description = "Porta do daemon SSH na VM."
  type        = number
  default     = 22
}

variable "ssh_private_key_path" {
  description = "Caminho absoluto ou relativo à máquina onde roda terraform para a chave privada SSH (não versionar no git)."
  type        = string
  sensitive   = true
}

variable "bootstrap_script_path" {
  description = "Caminho opcional para script customizado; default usa scripts/bootstrap.sh ao lado deste módulo."
  type        = string
  default     = null
}

variable "tailscale_funnel_enabled" {
  description = "Se true, após o bootstrap instala/configura Tailscale e publica o backend via Funnel (--bg) na VM por SSH."
  type        = bool
  default     = false
}

variable "funnel_https_port" {
  description = "Porta HTTPS na borda do Funnel (Tailscale aceita 443, 8443 ou 10000)."
  type        = number
  default     = 8443
}

variable "funnel_backend_port" {
  description = "Porta local na VM para onde o Funnel faz proxy (ex.: 80 para o stack Docker)."
  type        = number
  default     = 80
}

variable "tailscale_auth_key" {
  description = "Opcional. Auth key do painel Tailscale para 'tailscale up' não interativo. Se vazio, assume tailnet já autenticada na VM. Não commitar."
  type        = string
  sensitive   = true
  default     = ""
}

variable "public_base_url" {
  description = "Opcional. URL pública (ex.: https://nó.ts.net) só para documentação em output — não altera o Funnel."
  type        = string
  nullable    = true
  default     = null
}
