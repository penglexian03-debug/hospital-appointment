param(
    [int]$Port = 6379
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$client = 'C:\Users\19097\tools\memurai\4.1.2\Memurai\memurai-cli.exe'
$pidFile = Join-Path $projectRoot 'runtime\redis\memurai.pid'

if (Test-Path $client) {
    & $client -p $Port SHUTDOWN NOSAVE 2>$null | Out-Null
}

if (Test-Path $pidFile) {
    $serverPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    if ($serverPid) {
        $process = Get-Process -Id $serverPid -ErrorAction SilentlyContinue
        if ($process -and $process.Path -like '*memurai.exe') {
            Stop-Process -Id $serverPid -Force
        }
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

Write-Host "Redis-compatible server on port $Port has been stopped."
