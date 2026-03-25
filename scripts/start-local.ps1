Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot "backend"
$backendEnvScript = Join-Path $backendRoot "scripts\use-dev-env.ps1"
$backendStartScript = Join-Path $backendRoot "scripts\start-dev.ps1"
$backendStdout = Join-Path $backendRoot "logs\backend-stdout.log"
$frontendPort = 5273
$postgresPort = 5432
$redisPort = 6379
$frontendStdout = Join-Path $projectRoot "logs\frontend-stdout.log"
$frontendStderr = Join-Path $projectRoot "logs\frontend-stderr.log"
$viteCliPath = Join-Path $projectRoot "node_modules\vite\bin\vite.js"
$nodeExePath = "C:\Program Files\nodejs\node.exe"

if (Test-Path $backendEnvScript) {
  . $backendEnvScript
}

function Ensure-Directory {
  param([string]$Path)

  if (-not (Test-Path $Path)) {
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
  }
}

function Test-PortListening {
  param([int]$Port)

  $client = New-Object System.Net.Sockets.TcpClient
  try {
    $asyncResult = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
    $connected = $asyncResult.AsyncWaitHandle.WaitOne(1000, $false)
    if (-not $connected) {
      return $false
    }

    $client.EndConnect($asyncResult)
    return $true
  } catch {
    return $false
  } finally {
    $client.Close()
  }
}

function Wait-ForPort {
  param(
    [int]$Port,
    [int]$TimeoutSeconds = 60,
    [string]$DisplayName = "service"
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-PortListening -Port $Port) {
      return
    }
    Start-Sleep -Seconds 1
  }

  throw "$DisplayName failed to start on port $Port within $TimeoutSeconds seconds."
}

function Start-PostgresIfNeeded {
  if (Test-PortListening -Port $postgresPort) {
    Write-Output "[ok] PostgreSQL is already running on port $postgresPort."
    return
  }

  if (-not $env:PGROOT) {
    throw "PGROOT is not configured. Please run backend\\scripts\\persist-dev-env.ps1 once."
  }

  $postgresStartScript = Join-Path (Split-Path -Parent $env:PGROOT) "start-postgres.cmd"
  if (-not (Test-Path $postgresStartScript)) {
    throw "PostgreSQL start script was not found: $postgresStartScript"
  }

  Write-Output "[info] PostgreSQL is not running. Starting database..."
  & cmd.exe /c $postgresStartScript | Out-Null
  Wait-ForPort -Port $postgresPort -TimeoutSeconds 40 -DisplayName "PostgreSQL"
  Write-Output "[ok] PostgreSQL started."
}

function Start-RedisIfNeeded {
  if (Test-PortListening -Port $redisPort) {
    Write-Output "[ok] Redis is already running on port $redisPort."
    return
  }

  $redisService = Get-Service -Name "Redis" -ErrorAction SilentlyContinue
  if ($null -eq $redisService) {
    Write-Output "[warn] Redis service was not found. Backend may fail if Redis is required."
    return
  }

  Write-Output "[info] Redis is not running. Starting service..."
  Start-Service -Name "Redis"
  Wait-ForPort -Port $redisPort -TimeoutSeconds 20 -DisplayName "Redis"
  Write-Output "[ok] Redis started."
}

function Start-BackendIfNeeded {
  if (-not (Test-Path $backendStartScript)) {
    throw "Backend start script was not found: $backendStartScript"
  }

  Write-Output "[info] Rebuilding and restarting backend..."
  & powershell -ExecutionPolicy Bypass -File $backendStartScript
  if ($LASTEXITCODE -ne 0) {
    throw "Backend start script failed."
  }
  Write-Output "[ok] Backend started."
}

function Start-FrontendIfNeeded {
  if (Test-PortListening -Port $frontendPort) {
    Write-Output "[ok] Frontend is already running on port $frontendPort."
    return
  }

  if (-not (Test-Path $nodeExePath)) {
    throw "Node executable was not found: $nodeExePath"
  }

  if (-not (Test-Path $viteCliPath)) {
    throw "Vite CLI was not found. Please run npm install first."
  }

  Ensure-Directory -Path (Split-Path -Parent $frontendStdout)
  Remove-Item $frontendStdout, $frontendStderr -ErrorAction SilentlyContinue

  Write-Output "[info] Starting frontend..."
  Start-Process `
    -FilePath $nodeExePath `
    -ArgumentList $viteCliPath, "--host", "0.0.0.0", "--port", "$frontendPort" `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput $frontendStdout `
    -RedirectStandardError $frontendStderr `
    -WindowStyle Hidden | Out-Null

  Wait-ForPort -Port $frontendPort -TimeoutSeconds 20 -DisplayName "Frontend"
  Write-Output "[ok] Frontend started."
}

Start-PostgresIfNeeded
Start-RedisIfNeeded
Start-BackendIfNeeded
Start-FrontendIfNeeded

Write-Output ""
Write-Output "Project is ready:"
Write-Output "  Frontend: http://localhost:$frontendPort"
Write-Output "  Backend : http://localhost:8083/api/v1/system/ping"
Write-Output "  Frontend log: $frontendStdout"
Write-Output "  Backend log : $backendStdout"
