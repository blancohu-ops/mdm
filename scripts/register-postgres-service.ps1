$ErrorActionPreference = "Stop"

$pgRoot = "E:\tools\postgresql-18.3\pgsql"
$dataDir = "E:\data\postgresql\18\data"
$logDir = "E:\data\postgresql\18\log"
$serviceName = "PostgreSQL-MDM-18"
$serviceDisplayName = "PostgreSQL MDM 18"
$serviceDescription = "Local PostgreSQL service for the MDM development environment."
$pgCtl = Join-Path $pgRoot "bin\pg_ctl.exe"
$pgIsReady = Join-Path $pgRoot "bin\pg_isready.exe"

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Path $pgCtl)) {
    throw "pg_ctl.exe not found: $pgCtl"
}

if (-not (Test-Path $dataDir)) {
    throw "PostgreSQL data directory not found: $dataDir"
}

if (-not (Test-IsAdministrator)) {
    throw "This script must be run as Administrator."
}

if (-not (Test-Path $logDir)) {
    New-Item -Path $logDir -ItemType Directory -Force | Out-Null
}

if ((Test-Path $pgIsReady) -and (& $pgIsReady -h localhost -p 5432 *> $null; $LASTEXITCODE -eq 0)) {
    & $pgCtl stop -D $dataDir -w
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to stop the existing PostgreSQL instance before switching to service mode."
    }
}

$existingService = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $existingService) {
    & $pgCtl register `
        -N $serviceName `
        -D $dataDir `
        -S auto
    if ($LASTEXITCODE -ne 0) {
        throw "pg_ctl register failed with exit code $LASTEXITCODE"
    }
}

sc.exe description "$serviceName" "$serviceDescription" | Out-Null
sc.exe config "$serviceName" start= auto DisplayName= "$serviceDisplayName" | Out-Null
Start-Service -Name $serviceName
Get-Service -Name $serviceName | Select-Object Name, Status, StartType
