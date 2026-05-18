# Carrega variáveis do .env na sessão atual do PowerShell.
# Uso (na raiz do projeto):
#   . .\scripts\load-env.ps1
#   .\mvnw.cmd spring-boot:run

$root = Split-Path $PSScriptRoot -Parent
$envPath = Join-Path $root ".env"

if (-not (Test-Path $envPath)) {
    Write-Host ""
    Write-Host "Arquivo .env nao encontrado em:" -ForegroundColor Red
    Write-Host "  $envPath"
    Write-Host ""
    Write-Host "Faca:" -ForegroundColor Yellow
    Write-Host "  copy .env.example .env"
    Write-Host "  notepad .env"
    Write-Host ""
    Write-Host "Preencha IFOOD_CLIENT_ID, IFOOD_CLIENT_SECRET, IFOOD_REFRESH_TOKEN e IFOOD_MERCHANT_ID"
    Write-Host "Veja README.MD -> Caminho B -> Passo 3b"
    exit 1
}

$loaded = @()
Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $eq = $line.IndexOf("=")
    if ($eq -lt 1) { return }
    $name = $line.Substring(0, $eq).Trim()
    $value = $line.Substring($eq + 1).Trim().Trim('"').Trim("'")
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
    $loaded += $name
}

Write-Host ""
Write-Host "Variaveis carregadas de: $envPath" -ForegroundColor Green

$required = @(
    "IFOOD_CLIENT_ID",
    "IFOOD_CLIENT_SECRET",
    "IFOOD_REFRESH_TOKEN",
    "IFOOD_MERCHANT_ID"
)

$missing = $required | Where-Object {
    -not $loaded.Contains($_) -or [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process"))
}

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "ATENCAO — vazios ou ausentes no .env:" -ForegroundColor Yellow
    $missing | ForEach-Object { Write-Host "  - $_" }
    Write-Host ""
    Write-Host "Modo real (MOCK=false) vai falhar ate preencher. README -> Passo 3b"
}

$mock = [Environment]::GetEnvironmentVariable("IFOOD_MOCK_ENABLED", "Process")
$polling = [Environment]::GetEnvironmentVariable("IFOOD_POLLING_ENABLED", "Process")
Write-Host "  MOCK=$mock  POLLING=$polling" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proximo: .\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host ""
