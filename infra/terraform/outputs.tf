output "bootstrap_resource_id" {
  description = "ID do null_resource após apply (muda se triggers mudarem)."
  value       = null_resource.vm_bootstrap.id
}

output "bootstrap_script_sha256" {
  description = "Hash do script de bootstrap usado neste apply."
  value       = filesha256(local.bootstrap_script)
}

output "ssh_target" {
  description = "Alvo SSH configurado (sem segredos)."
  value       = "${var.ssh_user}@${var.ssh_host}:${var.ssh_port}"
}

output "public_base_url" {
  description = "URL pública opcional informada manualmente (documentação). Não é criada pelo Terraform."
  value       = var.public_base_url
}

output "tailscale_funnel" {
  description = "Resumo quando tailscale_funnel_enabled = true (sem segredos)."
  value = var.tailscale_funnel_enabled ? {
    resource_id         = null_resource.tailscale_funnel[0].id
    https_port          = var.funnel_https_port
    backend_port        = var.funnel_backend_port
    auth_key_configured = length(var.tailscale_auth_key) > 0
  } : null
}
