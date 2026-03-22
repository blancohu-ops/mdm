. "$PSScriptRoot\use-dev-env.ps1"

$projectRoot = 'E:\workspace\mdm'
$backendRoot = Join-Path $projectRoot 'backend'
$jarPath = Join-Path $backendRoot 'target\mdm-backend-0.1.0-SNAPSHOT.jar'
$logDir = 'E:\workspace\mdm\backend\logs'
if (-not (Test-Path $logDir)) {
  New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

$stdout = Join-Path $logDir 'backend-stdout.log'
$stderr = Join-Path $logDir 'backend-stderr.log'

$buildOk = $true
Push-Location $projectRoot
try {
  & mvn -f backend\pom.xml -DskipTests package
  if ($LASTEXITCODE -ne 0) {
    $buildOk = $false
  }
} finally {
  Pop-Location
}

if (-not $buildOk) {
  throw 'Backend package failed. See the Maven output above.'
}

$process = Start-Process -FilePath 'powershell.exe' `
  -ArgumentList '-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', ". '$PSScriptRoot\use-dev-env.ps1'; Set-Location '$projectRoot'; & '$env:JAVA_HOME\bin\java.exe' '-jar' '$jarPath' '--spring.profiles.active=dev'" `
  -PassThru `
  -WindowStyle Hidden `
  -RedirectStandardOutput $stdout `
  -RedirectStandardError $stderr

"PID=$($process.Id)"
"STDOUT=$stdout"
"STDERR=$stderr"
