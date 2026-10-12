param([switch]$Force)

$ErrorActionPreference = "Stop"
$target = Join-Path $PSScriptRoot ".env"
if ((Test-Path -LiteralPath $target) -and -not $Force) {
    Write-Host ".env already exists; no changes made. Use -Force to regenerate it."
    exit 0
}

function New-Secret([int]$Length = 32) {
    $bytes = [System.Security.Cryptography.RandomNumberGenerator]::GetBytes($Length)
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$content = @(
    "DB_PASSWORD=$(New-Secret 24)"
    "DB_ROOT_PASSWORD=$(New-Secret 32)"
    "JWT_SECRET=$(New-Secret 48)"
    "PUBLIC_BASE_URL=http://localhost:8088"
    "WEB_PORT=8088"
    "MYSQL_PORT=3307"
    "MAX_GROUP_BYTES=524288000"
    "PICKUP_RATE_LIMIT=60"
    "AUTH_RATE_LIMIT=30"
) -join [Environment]::NewLine

[IO.File]::WriteAllText($target, $content + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
Write-Host "Created local deploy/.env with generated secrets."
