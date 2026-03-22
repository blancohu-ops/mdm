param(
  [string]$ManifestPath = (Join-Path $PSScriptRoot 'manifest.json'),
  [string]$OutputDir = (Join-Path $PSScriptRoot 'downloaded')
)

if (-not (Test-Path $ManifestPath)) {
  throw "Manifest not found: $ManifestPath"
}

$items = Get-Content $ManifestPath -Raw | ConvertFrom-Json
if (-not (Test-Path $OutputDir)) {
  New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$results = foreach ($item in $items) {
  $folderName = ('{0:D2}_{1}' -f [int]$item.idx, $item.screenId.Substring(0, 8))
  $dir = Join-Path $OutputDir $folderName
  if (-not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
  }

  $imgPath = Join-Path $dir 'screenshot.png'
  $htmlPath = Join-Path $dir 'screen.html'

  & curl.exe -L --fail --silent --show-error $item.screenshotUrl -o $imgPath
  if ($LASTEXITCODE -ne 0) {
    throw "Failed screenshot download for $($item.screenId)"
  }

  & curl.exe -L --fail --silent --show-error $item.htmlUrl -o $htmlPath
  if ($LASTEXITCODE -ne 0) {
    throw "Failed html download for $($item.screenId)"
  }

  [PSCustomObject]@{
    idx = $item.idx
    title = $item.title
    screenId = $item.screenId
    screenshot = $imgPath
    screenshotBytes = (Get-Item $imgPath).Length
    html = $htmlPath
    htmlBytes = (Get-Item $htmlPath).Length
  }
}

$results | Select-Object idx, title, screenId, screenshotBytes, htmlBytes | Format-Table -AutoSize
