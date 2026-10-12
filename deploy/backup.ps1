param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "backups")
)

$ErrorActionPreference = "Stop"
$resolvedDeploy = (Resolve-Path -LiteralPath $PSScriptRoot).Path
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$resolvedOutput = (Resolve-Path -LiteralPath $OutputDirectory).Path
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$target = Join-Path $resolvedOutput "team-toolbox-$stamp"
New-Item -ItemType Directory -Path $target | Out-Null

$usePlugin = $false
try { docker compose version *> $null; $usePlugin = $LASTEXITCODE -eq 0 } catch { $usePlugin = $false }
function Invoke-Compose { param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments); if ($usePlugin) { & docker compose @Arguments } else { & docker-compose @Arguments } }

Push-Location $resolvedDeploy
try {
    Invoke-Compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers team_toolbox' |
        Set-Content -LiteralPath (Join-Path $target "database.sql") -Encoding utf8
    Invoke-Compose cp backend:/data/storage/. (Join-Path $target "storage")
    Copy-Item -LiteralPath (Join-Path $resolvedDeploy "docker-compose.yml") -Destination $target
    Write-Host "Backup created: $target"
} finally {
    Pop-Location
}
