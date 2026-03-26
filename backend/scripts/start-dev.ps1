param(
  [string]$Profile = 'dev',
  [switch]$SkipPackage
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\use-dev-env.ps1"

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backendRoot = Join-Path $projectRoot 'backend'
$jarPath = Join-Path $backendRoot 'target\mdm-backend-0.1.0-SNAPSHOT.jar'
$logDir = Join-Path $backendRoot 'logs'
$stdout = Join-Path $logDir 'backend-stdout.log'
$stderr = Join-Path $logDir 'backend-stderr.log'
$healthUrl = 'http://localhost:8083/actuator/health'
$stopScript = Join-Path $PSScriptRoot 'stop-dev.ps1'

function Ensure-Directory {
  param([Parameter(Mandatory = $true)][string]$Path)

  if (-not (Test-Path $Path)) {
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
  }
}

function Wait-BackendReady {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Url,
    [int]$TimeoutSeconds = 60
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $health = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 3 -ErrorAction Stop
      if ($health.status -eq 'UP') {
        return
      }
    } catch {
      Start-Sleep -Seconds 1
    }
  }

  throw "Backend failed to become healthy within $TimeoutSeconds seconds: $Url"
}

Ensure-Directory -Path $logDir

if (Test-Path $stopScript) {
  & $stopScript | Out-Null
}

if (-not $SkipPackage) {
  Push-Location $projectRoot
  try {
    Write-Output '[info] Packaging backend...'
    & mvn -f backend\pom.xml -DskipTests package
    if ($LASTEXITCODE -ne 0) {
      throw 'Backend package failed. See Maven output above.'
    }
  } finally {
    Pop-Location
  }
}

if (-not (Test-Path $jarPath)) {
  throw "Backend jar not found: $jarPath"
}

Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue

$command =
  ". '$PSScriptRoot\use-dev-env.ps1'; " +
  "Set-Location '$projectRoot'; " +
  "& '$env:JAVA_HOME\bin\java.exe' '-jar' '$jarPath' '--spring.profiles.active=$Profile'"

Write-Output '[info] Starting backend...'
$process = Start-Process -FilePath 'powershell.exe' `
  -ArgumentList '-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $command `
  -PassThru `
  -WindowStyle Hidden `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr

Wait-BackendReady -Url $healthUrl

$jarInfo = Get-Item $jarPath

"PID=$($process.Id)"
"PROFILE=$Profile"
"JAR=$jarPath"
"JAR_LAST_WRITE=$($jarInfo.LastWriteTime.ToString('s'))"
"STDOUT=$stdout"
"STDERR=$stderr"
