param(
    [Parameter(Mandatory = $true)][string]$BackupDirectory,
    [switch]$ConfirmDataReplacement
)

$ErrorActionPreference = "Stop"
if (-not $ConfirmDataReplacement) { throw "Restore replaces current database and files. Re-run with -ConfirmDataReplacement." }
$resolvedBackup = (Resolve-Path -LiteralPath $BackupDirectory).Path
$sqlFile = Join-Path $resolvedBackup "database.sql"
$storageDirectory = Join-Path $resolvedBackup "storage"
if (-not (Test-Path -LiteralPath $sqlFile -PathType Leaf)) { throw "Missing database.sql in backup." }
if (-not (Test-Path -LiteralPath $storageDirectory -PathType Container)) { throw "Missing storage directory in backup." }

$usePlugin = $false
try { docker compose version *> $null; $usePlugin = $LASTEXITCODE -eq 0 } catch { $usePlugin = $false }
function Invoke-Compose { param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments); if ($usePlugin) { & docker compose @Arguments } else { & docker-compose @Arguments } }

Push-Location $PSScriptRoot
try {
    Get-Content -LiteralPath $sqlFile -Raw | Invoke-Compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" team_toolbox'
    Invoke-Compose exec -T backend sh -c 'find /data/storage -mindepth 1 -delete'
    Invoke-Compose cp "$storageDirectory/." backend:/data/storage/
    Invoke-Compose restart backend
    Write-Host "Restore completed from: $resolvedBackup"
} finally {
    Pop-Location
}
