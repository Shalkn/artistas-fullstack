locals {
  bootstrap_script = coalesce(
    var.bootstrap_script_path,
    "${path.module}/scripts/bootstrap.sh"
  )

  funnel_script_content = templatefile("${path.module}/scripts/tailscale-funnel.sh.tpl", {
    auth_key_b64 = base64encode(var.tailscale_auth_key)
    https_port   = var.funnel_https_port
    backend_port = var.funnel_backend_port
  })
}

resource "null_resource" "vm_bootstrap" {
  triggers = {
    bootstrap_hash = filesha256(local.bootstrap_script)
    ssh_host       = var.ssh_host
    ssh_user       = var.ssh_user
    ssh_port       = tostring(var.ssh_port)
  }

  connection {
    type        = "ssh"
    user        = var.ssh_user
    host        = var.ssh_host
    port        = var.ssh_port
    private_key = file(pathexpand(var.ssh_private_key_path))
  }

  provisioner "file" {
    source      = local.bootstrap_script
    destination = "/tmp/terraform-bootstrap.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /tmp/terraform-bootstrap.sh",
      "sudo DEBIAN_FRONTEND=noninteractive /tmp/terraform-bootstrap.sh",
      "rm -f /tmp/terraform-bootstrap.sh",
    ]
  }
}

resource "null_resource" "tailscale_funnel" {
  count = var.tailscale_funnel_enabled ? 1 : 0

  depends_on = [null_resource.vm_bootstrap]

  triggers = {
    funnel_tpl_hash = filesha256("${path.module}/scripts/tailscale-funnel.sh.tpl")
    https_port      = tostring(var.funnel_https_port)
    backend_port    = tostring(var.funnel_backend_port)
    # Nova auth key: altere também este marcador ou rode terraform apply -replace para forçar reprovisionamento.
    auth_marker = length(var.tailscale_auth_key) > 0 ? "with_auth" : "no_auth"
  }

  connection {
    type        = "ssh"
    user        = var.ssh_user
    host        = var.ssh_host
    port        = var.ssh_port
    private_key = file(pathexpand(var.ssh_private_key_path))
  }

  provisioner "file" {
    content     = local.funnel_script_content
    destination = "/tmp/terraform-tailscale-funnel.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /tmp/terraform-tailscale-funnel.sh",
      "sudo /tmp/terraform-tailscale-funnel.sh",
      "rm -f /tmp/terraform-tailscale-funnel.sh",
    ]
  }
}
