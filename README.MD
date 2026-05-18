# iFood Pedidos — Restaurante & Lanchonete

Sistema Spring Boot que **recebe pedidos do iFood**, grava no MySQL e exibe na cozinha em tempo real.

| O quê | URL |
|-------|-----|
| **Tela da cozinha (KDS)** | http://localhost:8080/pedidos |
| Cardápio (produtos do cliente) | https://portal.ifood.com.br/menu/list |
| Pedidos no iFood | https://portal.ifood.com.br/orders |
| Simular pedido (dev) | https://developer.ifood.com.br/pt-BR/developer/tests |

---

## Índice

1. [Comece aqui — escolha o caminho](#comece-aqui--escolha-o-caminho)
2. [O que este sistema faz](#o-que-este-sistema-faz)
3. [Antes de começar](#antes-de-começar)
4. [Caminho A — Teste rápido sem iFood (5 min)](#caminho-a--teste-rápido-sem-ifood-5-min)
5. [Caminho B — Integração real com iFood](#caminho-b--integração-real-com-ifood)
6. [Próximas execuções (já tem `.env`)](#próximas-execuções-já-tem-env)
7. [Cardápio e imagens](#cardápio-e-imagens)
8. [Variáveis de ambiente](#variáveis-de-ambiente)
9. [Problemas comuns](#problemas-comuns)
10. [Fluxo na tela (Confirmar → Concluir)](#fluxo-na-tela-confirmar--concluir)
11. [Endpoints da API](#endpoints-da-api)
12. [Renovar token](#renovar-token)
13. [Rodar pelo IDE (VS Code / Cursor)](#rodar-pelo-ide-vs-code--cursor)
14. [Links úteis](#links-úteis)

---

## Comece aqui — escolha o caminho

| Caminho | Para quem | Tempo | Precisa de conta iFood Developer? |
|---------|-----------|-------|-----------------------------------|
| **[A — Teste rápido](#caminho-a--teste-rápido-sem-ifood-5-min)** | Ver a tela funcionando sem API | ~5 min | Não |
| **[B — Integração real](#caminho-b--integração-real-com-ifood)** | Pedido do iFood caindo em `/pedidos` | ~20–30 min (1ª vez) | Sim |

> **Primeira vez com iFood?** Siga o **Caminho B**.  
> **Só quer ver a interface?** Siga o **Caminho A** e depois volte ao B.

---

## O que este sistema faz

```text
┌─────────────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│ Portal iFood        │     │ API iFood        │     │ Este projeto        │
│ (cardápio, pedidos) │ ──► │ (eventos/pedidos)│ ──► │ localhost:8080      │
│ menu/list, /orders  │     │ polling 15–30s │     │ /pedidos (cozinha)  │
└─────────────────────┘     └──────────────────┘     └─────────────────────┘
```

| Faz | Não faz |
|-----|---------|
| Recebe pedido novo do iFood | Cadastrar produtos no MySQL |
| Mostra itens, valor, cliente | Substituir o app iFood do cliente |
| Confirmar, preparar, pronto, despachar | Enviar cardápio/fotos para o app |

O cliente **pede no app iFood**. A loja **opera em** http://localhost:8080/pedidos.

---

## Antes de começar

### Checklist

- [ ] **Java 17** instalado (`java -version`)
- [ ] **MySQL** rodando (usuário `root`, senha `12345` — ou ajuste no `.env`)
- [ ] **PowerShell** (Windows) ou terminal equivalente
- [ ] (Caminho B) Conta no [iFood Developer](https://developer.ifood.com.br/pt-BR/developer)
- [ ] (Caminho B) App tipo **distribuído** criado (não usa `client_credentials`)
- [ ] (Caminho B) **Loja de teste** vinculada ao app

### Onde pegar cada credencial (Caminho B)

| O que | Onde no portal |
|-------|----------------|
| `IFOOD_CLIENT_ID` | Developer → **Meus aplicativos** → seu app → Client ID |
| `IFOOD_CLIENT_SECRET` | Mesma tela → Client Secret (copiar; se vazar, regenere) |
| `IFOOD_MERCHANT_ID` | Developer → **Lojas de teste** → sua loja → UUID |
| `IFOOD_REFRESH_TOKEN` | Gerado no [Passo 3](#passo-3--obter-refresh-token-oauth) (não aparece pronto no portal) |

> **Segurança:** nunca commite `.env`, `CLIENT_SECRET` ou `REFRESH_TOKEN`. O arquivo `.env` já está no `.gitignore`.

---

## Caminho A — Teste rápido sem iFood (5 min)

Ideal para validar MySQL, Spring e a tela **sem** configurar OAuth.

### A.1 — Subir o projeto

```powershell
cd C:\caminho\para\springboot-rabbitmq
.\mvnw.cmd spring-boot:run
```

(Padrão: `IFOOD_MOCK_ENABLED=true`, polling desligado.)

### A.2 — Simular um pedido

**Opção 1 — Postman ou curl**

`POST` http://localhost:8080/webhook/ifood

```json
{
  "id": "evt_001",
  "code": "CONFIRMED",
  "orderId": "pedido-teste-1",
  "createdAt": "2026-05-18T12:00:00Z"
}
```

**Opção 2 — Script Node (com app rodando)**

```powershell
npm install
node load-test.js fluxo 1
```

### A.3 — Ver na tela

Abra http://localhost:8080/pedidos — deve aparecer um card **Aguardando confirmação**. Clique nos botões para avançar o fluxo.

---

## Caminho B — Integração real com iFood

### Visão geral

| Passo | O que faz |
|-------|-----------|
| 0 | MySQL + pasta do projeto |
| 1 | Gera `userCode` (OAuth) |
| 2 | Autoriza no portal iFood |
| 3 | Troca código por `refresh_token` |
| 3b | Salva credenciais no arquivo `.env` |
| 4 | Sobe o Spring com polling ligado |
| 5 | Gera pedido de teste no iFood |
| 6 | Opera em `/pedidos` |

Use **um único PowerShell** nos passos 1–3 (o `CODE_VERIFIER` some se fechar a janela).

---

### Passo 0 — Preparar ambiente

```powershell
cd C:\caminho\para\springboot-rabbitmq
```

- MySQL ligado.
- Banco `ifood_pedidos` é criado automaticamente na primeira subida.

---

### Passo 1 — Gerar código OAuth (userCode)

Substitua `SEU_CLIENT_ID` pelo valor do Developer:

```powershell
$env:IFOOD_CLIENT_ID = "SEU_CLIENT_ID"

$link = Invoke-RestMethod `
  -Uri "https://merchant-api.ifood.com.br/authentication/v1.0/oauth/userCode" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{ clientId = $env:IFOOD_CLIENT_ID }

$env:IFOOD_CODE_VERIFIER = $link.authorizationCodeVerifier

Write-Host ""
Write-Host "=== VALIDO ~10 MINUTOS ===" -ForegroundColor Green
Write-Host "Codigo userCode:" $link.userCode
Write-Host ""
Write-Host "=== ABRA ESTE LINK ===" -ForegroundColor Yellow
Write-Host $link.verificationUrlComplete
Write-Host ""
```

**Anote:** o link abre algo como `https://portal.ifood.com.br/apps/code?c=XXXX-XXXX`.

---

### Passo 2 — Autorizar no portal

1. Abra o **link** impresso no Passo 1 (não digite o código manualmente se puder clicar no link).
2. Login com a conta da **loja de teste** (parceiro).
3. Clique em **Autorizar** o aplicativo.
4. Na tela seguinte, clique em **Copiar** no código (ex.: `RNCX-QTJQ`).

| Código | Quando usar |
|--------|-------------|
| `DHQK-GKMD` (ex.) do Passo 1 | Só para abrir o link — **não** use no Passo 3 |
| `RNCX-QTJQ` (ex.) **depois** de autorizar | Este vai no Passo 3 |

**Atenção:** não cole espaço no final do código (causa erro **401**).

---

### Passo 3 — Obter refresh token (OAuth)

No **mesmo PowerShell** do Passo 1:

```powershell
$env:IFOOD_CLIENT_SECRET = "SEU_CLIENT_SECRET_DO_PORTAL"
$env:IFOOD_AUTH_CODE = "CODIGO-COPIADO-DO-PORTAL".Trim()

$token = Invoke-RestMethod `
  -Uri "https://merchant-api.ifood.com.br/authentication/v1.0/oauth/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grantType                 = "authorization_code"
    clientId                  = $env:IFOOD_CLIENT_ID
    clientSecret              = $env:IFOOD_CLIENT_SECRET
    authorizationCode         = $env:IFOOD_AUTH_CODE
    authorizationCodeVerifier = $env:IFOOD_CODE_VERIFIER
  }

if (-not $token.accessToken) { throw "Falhou - verifique secret, codigo e se Passo 1 foi na mesma janela" }

$env:IFOOD_REFRESH_TOKEN = $token.refreshToken
Write-Host "SUCESSO! Guarde o refresh token no .env (Passo 3b)." -ForegroundColor Green
```

Só continue se aparecer **`SUCESSO!`** sem erro `401` antes.

---

### Passo 3b — Criar arquivo `.env` (recomendado)

Assim você não precisa refazer OAuth a cada reinício.

```powershell
copy .env.example .env
notepad .env
```

Preencha no `.env` (uma linha por variável, **sem aspas**):

```env
IFOOD_MOCK_ENABLED=false
IFOOD_POLLING_ENABLED=true
IFOOD_POLLING_INTERVAL_MS=15000

IFOOD_CLIENT_ID=seu-client-id
IFOOD_CLIENT_SECRET=seu-client-secret
IFOOD_REFRESH_TOKEN=cole-o-refresh-token-do-passo-3
IFOOD_MERCHANT_ID=uuid-da-loja-de-teste

DB_USERNAME=root
DB_PASSWORD=12345
```

| Campo | Dica |
|-------|------|
| `IFOOD_REFRESH_TOKEN` | String longa retornada em `$token.refreshToken` — copie inteira, uma linha |
| `IFOOD_MERCHANT_ID` | UUID da loja no Developer (não é o Client ID) |

Salve o arquivo. O `.env` **não** vai para o Git.

---

### Passo 4 — Subir o Spring (API real + polling)

**Opção recomendada — usar `.env`:**

```powershell
cd C:\caminho\para\springboot-rabbitmq
. .\scripts\load-env.ps1
.\mvnw.cmd spring-boot:run
```

Deve aparecer: `Variáveis carregadas de: ...\.env`

**Opção manual — variáveis na sessão** (se acabou de fazer Passo 3 e ainda não criou `.env`):

```powershell
$env:IFOOD_MOCK_ENABLED = "false"
$env:IFOOD_POLLING_ENABLED = "true"
$env:IFOOD_POLLING_INTERVAL_MS = "15000"
$env:IFOOD_MERCHANT_ID = "SEU_MERCHANT_UUID"
# CLIENT_ID, CLIENT_SECRET e REFRESH_TOKEN já na sessão do Passo 3

.\mvnw.cmd spring-boot:run
```

#### Como saber que o Passo 4 deu certo

| No log do Spring | Significado |
|------------------|-------------|
| `Access token iFood renovado` | Login na API OK |
| `Polling iFood: N evento(s)` | Pedido chegando |
| Erro `IFOOD_REFRESH_TOKEN inválido` | `.env` incompleto ou placeholder |
| Erro `401` | Secret ou refresh token errado |

Aguarde o log `Started IfoodPedidosApplication` antes do Passo 5.

---

### Passo 5 — Gerar pedido de teste

Com o Spring **rodando**, escolha:

| Opção | Como | Quando usar |
|-------|------|-------------|
| **A** | [Developer → Testes](https://developer.ifood.com.br/pt-BR/developer/tests) → gerar pedido de teste | Mais rápido; testa integração |
| **B** | App iFood com **e-mail de teste** cadastrado no Developer | Testa cardápio real (ex.: X-Burger) |

1. Gere um **pedido novo** (pedidos antigos do [portal /orders](https://portal.ifood.com.br/orders) **não** entram na fila retroativamente).
2. Aguarde **15–30 segundos** (intervalo do polling).
3. Abra http://localhost:8080/pedidos

#### Checklist — deu certo?

- [ ] Card do pedido na tela
- [ ] Nome do cliente e itens visíveis
- [ ] Botão **Confirmar pedido** disponível
- [ ] No portal iFood, o mesmo pedido existe em [/orders](https://portal.ifood.com.br/orders)

---

### Passo 6 — Operar na tela

No card do pedido, nesta ordem:

1. **Confirmar pedido** (prazo ~8 min no iFood real)
2. **Iniciar preparo**
3. **Marcar pronto**
4. **DELIVERY:** Despachar → Concluir | **TAKEOUT:** Concluir (sem Despachar)

A tela atualiza sozinha a cada 5 segundos.

---

## Próximas execuções (já tem `.env`)

Depois da primeira configuração (Caminho B, Passos 3b + 4):

```powershell
cd C:\caminho\para\springboot-rabbitmq
. .\scripts\load-env.ps1
.\mvnw.cmd spring-boot:run
```

Gere pedido novo em [Developer → Testes](https://developer.ifood.com.br/pt-BR/developer/tests) e abra http://localhost:8080/pedidos.

**Refresh token expirou ou 401?** Veja [Renovar token](#renovar-token) ou refaça [Passos 1–3](#passo-1--gerar-código-oauth-usercode).

---

## Cardápio e imagens

| Ação | Onde |
|------|------|
| Criar produto, preço, foto | https://portal.ifood.com.br/menu/list |
| Cliente pede no app | App iFood |
| Cozinha vê o pedido | http://localhost:8080/pedidos |

Fotos e nomes dos itens vêm do **iFood**, não deste projeto.

---

## Variáveis de ambiente

| Variável | Modo real | Modo mock (Caminho A) |
|----------|-----------|------------------------|
| `IFOOD_MOCK_ENABLED` | `false` | `true` (padrão) |
| `IFOOD_POLLING_ENABLED` | `true` | `false` (padrão) |
| `IFOOD_POLLING_INTERVAL_MS` | `15000` | — |
| `IFOOD_CLIENT_ID` | obrigatório | — |
| `IFOOD_CLIENT_SECRET` | obrigatório | — |
| `IFOOD_REFRESH_TOKEN` | obrigatório | — |
| `IFOOD_MERCHANT_ID` | UUID da loja | — |
| `DB_USERNAME` / `DB_PASSWORD` | opcional | opcional |

Arquivos:

| Arquivo | Uso |
|---------|-----|
| `.env.example` | Modelo — pode commitar |
| `.env` | Seus secrets — **não** commitar |
| `scripts/load-env.ps1` | Carrega `.env` no PowerShell |

---

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---------|----------------|---------|
| Pedido no [portal /orders](https://portal.ifood.com.br/orders), tela vazia | Mock ligado ou polling off | `.env` com `MOCK=false`, `POLLING=true` + `load-env.ps1` |
| `401` no Passo 3 | Espaço no código, secret errado, código expirado | `.Trim()`, secret atual, refazer Passos 1–3 na **mesma** janela |
| `SUCESSO!` mas antes teve `401` | PowerShell executou linhas após erro | Só confie se **não** houver `401` |
| `IFOOD_REFRESH_TOKEN inválido` | `.env` vazio ou `SEU_REFRESH_TOKEN` | Colar token real do Passo 3 |
| `Access token renovado` sem pedidos | Fila vazia | Gerar **pedido novo** (Passo 5) |
| IDE sem pedidos, terminal OK | IDE não lê `$env:` do PowerShell | [Rodar pelo IDE](#rodar-pelo-ide-vs-code--cursor) |
| Erro ao Confirmar (500/411) | API exige body JSON | Atualize o código e reinicie o Spring |
| `load-env.ps1` não acha `.env` | Arquivo não criado | `copy .env.example .env` na raiz do projeto |

---

## Fluxo na tela (Confirmar → Concluir)

| Evento iFood | Status na tela | Botão da loja |
|--------------|----------------|---------------|
| `CONFIRMED` / `PLC` | Aguardando confirmação | Confirmar |
| — | Confirmado | Iniciar preparo |
| `SEPARATION_STARTED` | Em preparo | — |
| `READY_TO_PICKUP` | Pronto | Marcar pronto |
| `DISPATCHED` | Despachado | Despachar (só DELIVERY) |
| `CONCLUDED` | Concluído | Concluir |
| `CANCELLED` | Cancelado | — |

> `CONFIRMED` no iFood = **pedido novo**, não “já confirmado pela loja”.

---

## Endpoints da API

| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/webhook/ifood` | Simular evento (Caminho A) |
| GET | `/pedidos` | Tela da cozinha |
| GET | `/api/pedidos` | JSON (`?apenasAtivos=true`) |
| POST | `/pedido/{id}/confirmar` | Confirmar (+ iFood se modo real) |
| POST | `/pedido/{id}/iniciar-preparo` | Iniciar preparo |
| POST | `/pedido/{id}/pronto` | Marcar pronto |
| POST | `/pedido/{id}/despachar` | Despachar (delivery) |
| POST | `/pedido/{id}/concluir` | Finalizar |

---

## Renovar token

Se o `refresh_token` ainda for válido (sem refazer portal):

```powershell
$env:IFOOD_CLIENT_ID = "SEU_CLIENT_ID"
$env:IFOOD_CLIENT_SECRET = "SEU_CLIENT_SECRET"
$env:IFOOD_REFRESH_TOKEN = "seu-refresh-token"

$r = Invoke-RestMethod `
  -Uri "https://merchant-api.ifood.com.br/authentication/v1.0/oauth/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grantType    = "refresh_token"
    clientId     = $env:IFOOD_CLIENT_ID
    clientSecret = $env:IFOOD_CLIENT_SECRET
    refreshToken = $env:IFOOD_REFRESH_TOKEN
  }

if ($r.refreshToken) {
  $r.refreshToken | Set-Clipboard
  Write-Host "Novo refresh token copiado — atualize o .env" -ForegroundColor Yellow
}
```

---

## Rodar pelo IDE (VS Code / Cursor)

O **Run/Debug do IDE não usa** o `.env` automaticamente nem o PowerShell onde você exportou variáveis.

1. Crie `.env` na raiz (Passo 3b).
2. Em **Run and Debug** → `launch.json` → adicione em `env`:

```json
"envFile": "${workspaceFolder}/.env"
```

Ou copie manualmente cada `IFOOD_*` do `.env` para `env` no `launch.json`.

3. Suba com **F5** e confira no log: `Access token iFood renovado`.

---

## Estrutura do projeto

```
com.example.ifood
├── config       → IfoodProperties
├── controller   → WebhookController, PedidoController
├── service      → PedidoService, IfoodPollingScheduler, IfoodAuthService, …
├── model        → Pedido, ItemPedido, StatusPedido
├── repository
└── dto
```

---

## Links úteis

- [iFood Developer](https://developer.ifood.com.br/pt-BR/developer)
- [Testes — simular pedido](https://developer.ifood.com.br/pt-BR/developer/tests)
- [Portal cardápio](https://portal.ifood.com.br/menu/list)
- [Portal pedidos](https://portal.ifood.com.br/orders)
- [OAuth app distribuído (documentação)](https://developer.ifood.com.br/pt-BR/docs/guides/modules/authentication/distributed/)
