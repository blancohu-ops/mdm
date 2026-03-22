$javaHome = 'E:\tools\jdk-21.0.10+7'
$pgRoot = 'E:\tools\postgresql-18.3\pgsql'
$storageRoot = 'E:/workspace/mdm/backend/storage'

function Set-UserEnvVar {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Name,
    [Parameter(Mandatory = $true)]
    [string]$Value
  )

  & reg.exe add 'HKCU\Environment' /v $Name /t REG_SZ /d $Value /f | Out-Null
  Set-Item -Path "Env:$Name" -Value $Value
}

function Add-UserPathEntry {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Entry
  )

  $currentUserPath = [Environment]::GetEnvironmentVariable('Path', 'User')
  $parts = @()
  if ($currentUserPath) {
    $parts = $currentUserPath.Split(';', [System.StringSplitOptions]::RemoveEmptyEntries)
  }

  $normalizedEntry = $Entry.TrimEnd('\')
  $exists = $parts | Where-Object { $_.TrimEnd('\') -ieq $normalizedEntry }
  if (-not $exists) {
    $parts = @($normalizedEntry) + $parts
    & reg.exe add 'HKCU\Environment' /v Path /t REG_EXPAND_SZ /d ($parts -join ';') /f | Out-Null
  }

  if (($env:Path.Split(';', [System.StringSplitOptions]::RemoveEmptyEntries) | Where-Object { $_.TrimEnd('\') -ieq $normalizedEntry }).Count -eq 0) {
    $env:Path = "$normalizedEntry;$env:Path"
  }
}

function Publish-EnvironmentChange {
  Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class NativeMethods {
    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
    public static extern IntPtr SendMessageTimeout(
        IntPtr hWnd,
        uint Msg,
        UIntPtr wParam,
        string lParam,
        uint fuFlags,
        uint uTimeout,
        out UIntPtr lpdwResult);
}
'@ | Out-Null

  $HWND_BROADCAST = [IntPtr]0xffff
  $WM_SETTINGCHANGE = 0x001A
  $SMTO_ABORTIFHUNG = 0x0002
  $result = [UIntPtr]::Zero
  [void][NativeMethods]::SendMessageTimeout(
    $HWND_BROADCAST,
    $WM_SETTINGCHANGE,
    [UIntPtr]::Zero,
    'Environment',
    $SMTO_ABORTIFHUNG,
    5000,
    [ref]$result
  )
}

Set-UserEnvVar -Name 'JAVA_HOME' -Value $javaHome
Set-UserEnvVar -Name 'PGROOT' -Value $pgRoot
Set-UserEnvVar -Name 'PGHOST' -Value 'localhost'
Set-UserEnvVar -Name 'PGPORT' -Value '5432'
Set-UserEnvVar -Name 'PGUSER' -Value 'postgres'
Set-UserEnvVar -Name 'PGPASSWORD' -Value 'postgres'
Set-UserEnvVar -Name 'PGDATABASE' -Value 'mdm_dev'
Set-UserEnvVar -Name 'REDISCLI_AUTH' -Value 'root'

Set-UserEnvVar -Name 'MDM_DB_URL' -Value 'jdbc:postgresql://localhost:5432/mdm_dev'
Set-UserEnvVar -Name 'MDM_DB_USERNAME' -Value 'postgres'
Set-UserEnvVar -Name 'MDM_DB_PASSWORD' -Value 'postgres'
Set-UserEnvVar -Name 'MDM_REDIS_HOST' -Value 'localhost'
Set-UserEnvVar -Name 'MDM_REDIS_PORT' -Value '6379'
Set-UserEnvVar -Name 'MDM_REDIS_PASSWORD' -Value 'root'
Set-UserEnvVar -Name 'MDM_STORAGE_ROOT' -Value $storageRoot
Set-UserEnvVar -Name 'MDM_JWT_SECRET' -Value 'mdm-dev-secret-key-2026-with-32-chars'
Set-UserEnvVar -Name 'MDM_SWAGGER_ENABLED' -Value 'true'

Add-UserPathEntry -Entry "$javaHome\bin"
Add-UserPathEntry -Entry "$pgRoot\bin"
Publish-EnvironmentChange

'Persisted user-level environment variables:'
'- JAVA_HOME'
'- PGROOT'
'- PGHOST / PGPORT / PGUSER / PGPASSWORD / PGDATABASE'
'- REDISCLI_AUTH'
'- MDM_DB_* / MDM_REDIS_* / MDM_STORAGE_ROOT / MDM_JWT_SECRET / MDM_SWAGGER_ENABLED'
'- Path += JAVA_HOME\\bin, PGROOT\\bin'
