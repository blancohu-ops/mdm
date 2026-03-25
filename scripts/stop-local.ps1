Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendStopScript = Join-Path $projectRoot "backend\scripts\stop-dev.ps1"

function Stop-ProcessesByPort {
  param([int]$Port)

  $processIds = netstat -ano | Select-String ":$Port" | ForEach-Object {
    ($_ -split "\s+")[-1]
  } | Where-Object { $_ -match "^\d+$" } | Select-Object -Unique

  foreach ($processId in $processIds) {
    if ([int]$processId -gt 0) {
      Stop-Process -Id ([int]$processId) -Force -ErrorAction SilentlyContinue
    }
  }
}

if (Test-Path $backendStopScript) {
  & powershell -ExecutionPolicy Bypass -File $backendStopScript | Out-Null
}

$startLocalProcesses = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -eq "powershell.exe" -and $_.CommandLine -like "*scripts\\start-local.ps1*"
}

foreach ($process in $startLocalProcesses) {
  Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
}

$frontendProcesses = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -eq "node.exe" -and $_.CommandLine -like "*node_modules\\vite\\bin\\vite.js*--port 5273*"
}

foreach ($process in $frontendProcesses) {
  Stop-Process -Id $process.ProcessId -Force
}

Stop-ProcessesByPort -Port 5273
Stop-ProcessesByPort -Port 8083

Write-Output "Local frontend/backend processes have been stopped."
