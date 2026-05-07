# Laboratório: Terraform + CI/CD no GitHub — guia de implementação

Este documento acompanha o plano **Lab Terraform CI/CD seguro** (VM local, Docker Compose, Terraform e GitHub Actions). O foco é **estudo**: você decide como implementar cada parte; aqui estão **ordem**, **critérios de pronto** e **onde termina configuração** e **onde começa código**.

**Repositório de referência:** [Shalkn/artistas-fullstack](https://github.com/Shalkn/artistas-fullstack)

---

## Como usar este guia

- **Checkpoint “configuração”**: você só configura VM, rede, ferramentas ou painéis — sem alterar o repositório (ou com mudanças mínimas).
- **Checkpoint “código / IaC”**: você cria ou altera arquivos no Git (`infra/`, `.github/workflows`, Compose, etc.).
- **Critério de pronto**: lista objetiva para saber se pode avançar.

Se algo falhar, anote o sintoma, o comando e o log — isso acelera quando você pedir ajuda depois.

---

## Visão geral do fluxo

1. VM local pronta e acessível por SSH com chave.
2. (Opcional mas recomendado) Exposição “externa” segura via túnel, não necessariamente IP público na Internet aberta.
3. Bootstrap da VM automatizado (Terraform e/ou cloud-init/Ansible — você escolhe o nível).
4. Stack da aplicação endurecida para ambiente de deploy (`compose` de produção/lab + `.env` fora do git).
5. CI no GitHub (lint, testes, build, Docker, validação Terraform).
6. CD controlado (SSH na VM, imagens no registry, deploy idempotente).
7. Trilhas de segurança para não vazar chaves, `.env` nem estado do Terraform.

---

## Fase 1 — VM local e acesso SSH

### Objetivo

Ter uma máquina “servidor” isolada, com SSH apenas por chave e rede compreensível (NAT vs bridge).

### O que estudar antes

- Diferença entre rede NAT e bridge no hipervisor.
- Autenticação SSH por par de chaves e por que desabilitar senha em servidor exposto.
- Conceito de “superfície de ataque” (portas abertas, serviços desnecessários).

### Passos sugeridos

1. Instalar hipervisor (VirtualBox, VMware ou equivalente) e criar VM **Ubuntu Server LTS** com disco e RAM razoáveis para Docker.
2. Escolher modo de rede:
   - **Bridge**: IP na mesma rede da sua LAN (simples para testar da sua máquina).
   - **NAT + redirecionamento de porta**: também válido para laboratório; entenda qual IP você usará nos manifests e no Terraform.
3. Instalar/atualizar pacotes base da VM; criar usuário não-root com `sudo`.
4. Configurar SSH: só chave pública, sem login direto de root por SSH (política comum em servidores).
5. **Checkpoint — só configuração:** firewall permitindo no mínimo SSH (e depois 80/443 se for expor HTTP). Ferramentas típicas: `ufw`, `firewalld` ou regras no hipervisor — escolha uma e documente sua decisão.
6. **Checkpoint — só configuração:** endurecimento leve — atualizações automáticas de segurança, `fail2ban` ou equivalente, timezone e NTP.

### Critérios de pronto (Fase 1)

- Você consegue `ssh usuario@ip-da-vm` **da sua máquina host** usando **chave**, sem senha interativa.
- Sabe qual é o **IP** que usará como “alvo” do Terraform e do CD (fixo na LAN ou documentado).
- Lista de portas expostas está **consciente** e justificada.

### Quando termina “configuração do servidor” aqui?

Quando SSH está estável, usuário e permissões estão corretos e a rede está definida. **Ainda não** é obrigatório ter Docker nem o app rodando.

---

## Fase 2 — “IP externo” sem virar alvo fácil

### Objetivo

Simular acesso “de fora” como em cloud, sem obrigatoriamente abrir port-forward agressivo no roteador no primeiro dia.

### O que estudar antes

- **Cloudflare Tunnel** ou **Tailscale Funnel**: tráfego chega via serviço gerenciado; você não precisa de IP público fixo residencial.
- Alternativa clássica: **port forwarding** no roteador + DNS dinâmico — maior responsabilidade de segurança.

### Passos sugeridos

1. Escolher **uma** estratégia de exposição e registrar no seu caderno de laboratório **por quê**.
2. Subir o agente do túnel na VM (ou no host, dependendo do modelo — entenda o fluxo na documentação oficial).
3. Apontar hostname ou URL de teste para o serviço HTTP da sua futura stack (por enquanto pode ser um placeholder tipo servidor web mínimo só para validar o túnel).

### Critérios de pronto (Fase 2)

- De uma rede externa (4G no celular, por exemplo), você acessa um serviço HTTP na VM **sem** depender do IP da LAN — ou, se optou só LAN primeiro, documentou explicitamente essa limitação e o próximo passo.

### Onde entra código?

**Nenhum obrigatório no repositório nesta fase**, salvo se você versionar um arquivo de configuração do túnel **sem segredos** — nesse caso, use placeholders e secrets no lugar certo (ver Fase 6).

---

## Fase 3 — Bootstrap da VM com Terraform (ou equivalente)

### Objetivo

Repetibilidade: destruir e recriar a VM sem “magia na cabeça”, instalando Docker e Compose de forma audível.

### O que estudar antes

- Ciclo `terraform init` / `plan` / `apply` / `destroy`.
- Diferença entre **provisionar a VM** (hipervisor/libvirt) e **provisionar software dentro da VM** (SSH remoto, cloud-init).
- Por que o arquivo `terraform.tfstate` é sensível.

### Passos sugeridos

1. **Checkpoint — código / IaC:** criar pasta `infra/terraform/` no repositório com módulos ou arquivos organizados (`variables`, `outputs`, recursos).
2. Decidir abordagem:
   - **A)** Terraform cria a VM local (provider de virt) **ou**
   - **B)** VM já existe e Terraform só faz **bootstrap remoto** por SSH (comum em laboratório).
3. Declarar variáveis: IP/host, usuário SSH, caminho de chave **local** (a chave privada **não** vai para o git — apenas referência via variável de ambiente ou arquivo fora do repo).
4. Implementar instalação de Docker + plugin Compose e pré-requisitos — via script renderizado, `remote-exec`, ou integração com Ansible/cloud-init (você pesquisa e escolhe).
5. **Checkpoint — código / IaC:** `outputs` úteis (host, usuário, talvez URL do túnel como variável externa).

### Critérios de pronto (Fase 3)

- `terraform validate` passa em máquina limpa (com providers instalados).
- Um `apply` deixa a VM com **Docker funcional** verificável remotamente.
- Você sabe **onde** está o `terraform.tfstate` e já aplicou **uma** regra de não commitar estado com segredos.

### Quando termina “configuração do servidor” nesta fase?

Quando o resultado do Terraform deixa a VM pronta para receber containers — não precisa ainda ter o `artistas-fullstack` rodando.

---

## Fase 4 — Runtime da aplicação (Compose “lab/prod”)

### Objetivo

O `docker-compose.yml` na raiz já sobe stack completo; para deploy na VM você quer **menos superfície** (menos portas publicadas) e políticas de restart/logs.

### O que estudar antes

- Compose override (`docker compose -f ... -f ...`).
- Diferença entre rede interna do Compose e portas publicadas no host.
- Healthchecks e ordem de subida (`depends_on` com condição de saúde, quando aplicável).

### Passos sugeridos

1. **Checkpoint — código:** criar `docker-compose.prod.yml` (nome sugerido) que **sobrescreve** o necessário: não expor Postgres/MinIO para a Internet se não for estritamente necessário; `restart`, limites de log, etc.
2. **Checkpoint — código:** adicionar `.env.example` na raiz com **somente placeholders** e descrição de cada variável (JWT, Postgres, MinIO, CORS para sua URL pública).
3. Na VM, criar `.env` **manualmente ou via mecanismo secreto** — arquivo que **nunca** entra no git.

### Critérios de pronto (Fase 4)

- Na VM, com `.env` correto, `docker compose` sobe API + web + dependências conforme seu desenho de rede.
- Do cliente externo (ou túnel), você acessa a aplicação e valida CORS/origins se alterou URLs.

### Onde termina configuração vs código?

- **Código no repo:** arquivos Compose de overlay + `.env.example` + documentação.
- **Configuração só na VM:** arquivo `.env` real e possivelmente certificados do túnel — **fora** do Git.

### Implementação neste repositório

- Overlay: [`docker-compose.prod.yml`](../docker-compose.prod.yml) — remove publicação de portas de `db`, `minio` e `api` no host; mantém só `web` em **80**; `restart: unless-stopped` e limite de logs.
- Template de variáveis: [`.env.example`](../.env.example) — copiar para `.env` na VM e preencher (inclua sua URL pública em `APP_CORS_ALLOWED_ORIGINS`).

**Comando na VM** (com repositório clonado na pasta do projeto):

```bash
cp .env.example .env
nano .env   # ajustar segredos e CORS
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env up -d --build
```

Acesso externo: apenas HTTP(S) na porta **80** do host (API via `/api` e `/ws` pelo Nginx do `web`, conforme `frontend/nginx.conf`).

---

## Fase 5 — CI no GitHub Actions

### Objetivo

Todo PR prova que o projeto **builda**, **testa** e que a **infra declarativa é válida**.

### O que estudar antes

- Eventos `pull_request` vs `push`.
- Cache de dependências (Maven/npm) para não desperdiçar minutos.
- Jobs paralelos vs sequenciais.

### Passos sugeridos

1. **Checkpoint — código:** criar `.github/workflows/ci.yml`:
   - backend: testes Maven (e/ou build).
   - frontend: lint + build.
   - opcional: `docker build` das imagens para pegitar erros de Dockerfile cedo.
2. **Checkpoint — código:** criar `.github/workflows/terraform-validate.yml`:
   - formatação, init sem backend remoto (se ainda não tiver backend), validate.
   - opcional: `tflint` — adiciona complexidade, mas é ótimo para portfólio.
3. No GitHub, configurar **branch protection** na `main`: exigir os checks do CI antes do merge.

### Critérios de pronto (Fase 5)

- PR de teste falha se você introduzir erro de sintaxe Terraform ou quebra de build.
- Você entende **qual permissão** o workflow precisa (somente leitura no repositório para CI puro).

### Segredos no CI?

Para **só validar** Terraform e rodar testes, normalmente **não** precisa de secrets. Evite adicionar cedo demais — reduz risco de vazamento por log.

### Implementação neste repositório (Fase 5)

- [`.github/workflows/ci.yml`](../.github/workflows/ci.yml): jobs paralelos — backend (`mvn verify`), frontend (`npm ci`, `lint`, `build`), Docker (`build` das imagens API/Web sem push, cache GHA).
- [`.github/workflows/terraform-validate.yml`](../.github/workflows/terraform-validate.yml): `terraform fmt -check`, `init -backend=false`, `validate -var-file=ci.tfvars` (usa [`infra/terraform/ci.tfvars`](../infra/terraform/ci.tfvars) + chave dummy no runner).

**Branch protection (manual no GitHub):** em **Settings → Branches**, proteja `main` e exija os status checks `CI`, `Terraform validate` (e outros que aparecerem) antes do merge.

---

## Fase 6 — CD (deploy na VM via SSH)

### Objetivo

Automatizar o que você faria manualmente: atualizar imagens e reiniciar Compose com segurança.

### O que estudar antes

- GitHub Container Registry (GHCR) ou Docker Hub — autenticação por token com escopo mínimo.
- **GitHub Environments** com aprovadores (deploy manual protegido).
- Alternativa moderna: **OIDC** para cloud — aqui, como é VM caseira, SSH com chave dedicada e passphrase via secret é caminho comum de laboratório.

### Passos sugeridos

1. **Checkpoint — código:** workflow de CD disparado por `workflow_dispatch` e/ou tag — você decide política.
2. Pipeline: build/push de imagens → SSH na VM → `docker compose pull` + `up -d`.
3. Pós-deploy: chamada a um health endpoint (`/actuator/health` no backend, conforme README principal do projeto) ou verificação de porta — falha deve marcar o job como vermelho.

### Critérios de pronto (Fase 6)

- Um deploy completo pode ser repetido sem drift manual não documentado.
- Credenciais existem **apenas** em GitHub Secrets / arquivo na VM, não no histórico do git.

### Implementação neste repositório (Fase 6)

- [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml): em **`workflow_dispatch`** ou **tag `v*`**: job **publish-ghcr** faz build e push das imagens `…-api` e `…-web` para **GHCR** (`ghcr.io/<owner>/<repo>-api` e `…-web`, tags `:sha` e `:latest`); job **deploy-vm** usa SSH e, se **`DEPLOY_PATH`** ainda não for um repo git, executa **`git clone`** de `https://github.com/<owner>/<repo>.git`; depois `git pull`, escreve `${{ secrets.DEPLOY_DOTENV }}` em `.env`, executa `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env up -d --build` e roda healthcheck do **Actuator** no container `api`.

**Secrets necessários no repositório** (Settings → Secrets and variables → Actions):

| Secret | Uso |
|--------|-----|
| `SSH_HOST` | Host que o **runner do Actions** consegue **rotear**. Runner **hospedado** (`ubuntu-latest`): use IP **público**, DDNS ou hostname na Internet (ex.: port forward SSH) — **não** basta IP Tailscale `100.x.x.x`. Runner **self-hosted** na mesma tailnet Tailscale: aí sim IP `100.x` ou hostname MagicDNS (`*.ts.net`). |
| `SSH_USER` | Usuário Linux na VM com permissão de `docker` (ou `sudo` conforme o workflow). |
| `SSH_PRIVATE_KEY` | Chave privada PEM (texto completo, incluindo `BEGIN`/`END`). O par público deve estar em `~/.ssh/authorized_keys` desse usuário na VM. |
| `DEPLOY_PATH` | **Caminho absoluto** na VM para a pasta raiz do repositório clonado (ex.: `/home/renan/artistas-fullstack`). Sem `~`; sem barra final obrigatória. É onde o workflow faz `git pull` e encontra `docker-compose.yml` e `.env`. |
| `DEPLOY_DOTENV` | Conteúdo completo do arquivo `.env` (multilinha) usado no deploy. O workflow recria esse arquivo em `${DEPLOY_PATH}/.env` a cada execução. |

### Checklist de conferência (antes do primeiro deploy)

Use esta lista para revisar configuração; marque os itens no seu fork quando estiverem ok (`[ ]` → `[x]` no Markdown).

**Rede e SSH (do seu PC até a VM)**

- [ ] Tailscale na VM online e na mesma tailnet que o cliente que testa SSH.
- [ ] No PC de teste: `tailscale status` mostra **logged in** (após reboot: `sudo tailscale up` se aparecer “Logged out”).
- [ ] `ssh -i ~/.ssh/<sua_chave> SSH_USER@<host>` funciona **antes** de confiar no Actions (host = IP Tailscale ou LAN, conforme o caso).

**GitHub Secrets (Settings → Secrets and variables → Actions)**

- [ ] `SSH_HOST` definido conforme o **tipo de runner** do job de deploy (ver tabela acima: hospedado ≠ Tailscale-only).
- [ ] `SSH_USER` igual ao usuário que você usa no SSH manual bem-sucedido.
- [ ] `SSH_PRIVATE_KEY` colado por inteiro; corresponde à chave autorizada na VM.
- [ ] `DEPLOY_PATH` como **caminho absoluto** na VM (ex.: `/home/renan/artistas-fullstack`), já decidido e conferido com `pwd` na pasta onde o clone deve ficar.
- [ ] `DEPLOY_DOTENV` cadastrado com o conteúdo completo do `.env` (multilinha), incluindo segredos e CORS corretos.

**Na VM**

- [ ] `git` e Docker (+ plugin Compose) funcionando.
- [ ] Usuário SSH com permissão de escrita em `DEPLOY_PATH` para o workflow atualizar `.env` no deploy.
- [ ] (Opcional até o primeiro run) Pasta `DEPLOY_PATH` já existe ou você aceita que o primeiro deploy crie o clone lá via `git clone`.

**Actions**

- [ ] Permissões do workflow para push no GHCR (`GITHUB_TOKEN` / Packages), conforme documentação do repositório.
- [ ] Primeiro disparo: `workflow_dispatch` ou tag `v*`, conforme [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml).

Opcional: criar **Environment** `production` no GitHub e exigir aprovadores antes do job `deploy-vm` (no workflow atual o environment não está fixado — você pode adicionar `environment: production` ao job quando configurar).

**Na VM (detalhe):** não é obrigatório clonar na mão: o primeiro deploy pode criar o clone em `DEPLOY_PATH`. O deploy **reconstrói** imagens com `--build`; as imagens no GHCR ficam disponíveis para uso futuro (ex.: deploy só com `pull`).

---

## Fase 7 — Segurança: não vazar chave, `.env` nem estado

### Objetivo

Repositório público **sem** vazamento de segredos e com hábitos de auditoria.

### Checklist (estudo + implementação)

1. **`.gitignore`** na raiz do repo: cobrir `.env`, `.env.*`, `*.pem`, `.terraform/`, `*.tfstate*`, pastas de credenciais de ferramentas de nuvem.
2. **Antes de cada commit:** revisar `git diff` com foco em strings que parecem JWT, senhas, tokens.
3. **GitHub:** Secrets por ambiente; variáveis públicas só para dados não sensíveis (ex.: nome do host da VM, se aceitável).
4. **Workflows:** não usar `echo $SECRET`; não passar segredos como argumentos visíveis em logs; preferir mecanismos recomendados pela documentação do Actions para SSH e registry.
5. **Terraform:** quando evoluir o laboratório, planejar **backend remoto** para estado e bloqueio — pesquisar motivos (colaboração, CI, drift).

### Critérios de pronto (Fase 7)

- Você consegue explicar em uma frase **onde** cada classe de segredo mora (VM vs GitHub vs sua máquina).
- Um scan rápido do histórico recente não mostra `.env` nem chaves privadas.

---

## Fase 8 — Portfólio e disciplina de PRs

### Objetivo

Demonstrar maturidade: infraestrutura e aplicação podem evoluir em PRs separados.

### Passos sugeridos

1. Abrir PR só com `infra/terraform` + validações.
2. Outro PR com workflows CI/CD.
3. Outro com Compose de deploy — assim você treina revisão focada.

### Critérios de pronto (Fase 8)

- README principal do projeto pode ganhar um link curto para este laboratório em `doc/` e um badge de CI (opcional).

---

## Definição de “projeto completo” (para você avaliar seu progresso)

| Área | Pronto quando |
|------|----------------|
| VM | SSH por chave, firewall consciente, atualizações |
| Exposição | Acesso externo ou decisão documentada de ficar só na LAN |
| IaC | Terraform valida e reproduz bootstrap |
| App | Compose de deploy com menos exposição que o dev |
| CI | PR obriga checks verdes |
| CD | Deploy repetível com secrets só nos lugares certos |
| Segurança | Sem `.env`/PEM/state sensível no git |

---

## Referências internas do repositório

- Compose atual: `docker-compose.yml` na raiz.
- README principal do sistema: `README.md` na raiz.

---

## Próximo passo recomendado

Começar pela **Fase 1** até o primeiro critério de pronto. Só então abrir a pasta `infra/terraform/` — ordem errada costuma gerar estado Terraform “amarrado” a uma VM que ainda muda de IP ou SSH.

Boa exploração; quando travar em algo concreto (erro de SSH, provider Terraform, permissão do Actions), traga o log e o que você já tentou.
