param(
  [string]$BaseUrl = 'http://localhost:8083',
  [string]$Account = 'enterprise@example.com',
  [string]$Password = 'Admin1234'
)

. "$PSScriptRoot\use-dev-env.ps1"

function Invoke-ApiJson {
  param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('GET', 'POST', 'PUT', 'DELETE')]
    [string]$Method,
    [Parameter(Mandatory = $true)]
    [string]$Url,
    [hashtable]$Headers,
    $Body
  )

  $request = @{
    Uri         = $Url
    Method      = $Method
    ErrorAction = 'Stop'
  }

  if ($Headers) {
    $request.Headers = $Headers
  }

  if ($null -ne $Body) {
    $request.ContentType = 'application/json'
    $request.Body = ($Body | ConvertTo-Json -Depth 10)
  }

  try {
    return Invoke-RestMethod @request
  } catch {
    $message = $_.Exception.Message
    if ($_.ErrorDetails.Message) {
      $message = "$message`n$($_.ErrorDetails.Message)"
    }
    throw $message
  }
}

function Wait-BackendReady {
  param(
    [Parameter(Mandatory = $true)]
    [string]$HealthUrl
  )

  for ($i = 0; $i -lt 30; $i++) {
    try {
      $health = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 3 -ErrorAction Stop
      if ($health.status -eq 'UP') {
        return
      }
    } catch {
      Start-Sleep -Seconds 1
    }
  }

  throw "Backend is not ready: $HealthUrl"
}

Wait-BackendReady -HealthUrl "$BaseUrl/actuator/health"

$login = Invoke-ApiJson -Method 'POST' -Url "$BaseUrl/api/v1/auth/login" -Body @{
  account  = $Account
  password = $Password
  remember = $false
}

$token = $login.data.accessToken
if (-not $token) {
  throw 'Login succeeded but accessToken is empty.'
}

$headers = @{
  Authorization = "Bearer $token"
}

$profile = Invoke-ApiJson -Method 'GET' -Url "$BaseUrl/api/v1/enterprise/profile" -Headers $headers
$products = Invoke-ApiJson -Method 'GET' -Url "$BaseUrl/api/v1/enterprise/products?page=1&pageSize=5" -Headers $headers
$messages = Invoke-ApiJson -Method 'GET' -Url "$BaseUrl/api/v1/enterprise/messages" -Headers $headers
$pendingProducts = Invoke-ApiJson -Method 'GET' -Url "$BaseUrl/api/v1/enterprise/products?status=pending_review&page=1&pageSize=1" -Headers $headers
$publishedProducts = Invoke-ApiJson -Method 'GET' -Url "$BaseUrl/api/v1/enterprise/products?status=published&page=1&pageSize=1" -Headers $headers

[pscustomobject]@{
  account             = $Account
  enterpriseId        = $profile.data.company.id
  enterpriseName      = $profile.data.company.name
  profileStatus       = $profile.data.company.status
  totalProducts       = $products.data.total
  unreadMessages      = $messages.data.unreadTotal
  pendingReviewTotal  = $pendingProducts.data.total
  publishedTotal      = $publishedProducts.data.total
  checkedAt           = (Get-Date).ToString('s')
} | ConvertTo-Json -Depth 10
