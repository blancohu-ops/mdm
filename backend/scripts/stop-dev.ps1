$processes = Get-CimInstance Win32_Process | Where-Object {
  $_.CommandLine -like '*mdm-backend-0.1.0-SNAPSHOT.jar*'
}

if (-not $processes) {
  'No backend dev process found.'
  exit 0
}

$processes | ForEach-Object {
  Stop-Process -Id $_.ProcessId -Force
  "Stopped PID=$($_.ProcessId)"
}
