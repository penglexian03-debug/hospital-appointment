param(
    [int]$Port = 6379
)

$client = 'C:\Users\19097\tools\memurai\4.1.2\Memurai\memurai-cli.exe'
if (-not (Test-Path $client)) {
    Write-Host "Memurai client not found."
    exit 1
}

$pong = & $client -p $Port PING 2>$null
if ($pong -eq 'PONG') {
    Write-Host "Redis-compatible server is running on 127.0.0.1:$Port."
    & $client -p $Port INFO server | Select-String -Pattern 'redis_version|memurai_version'
    exit 0
}

Write-Host "Redis-compatible server is not running on 127.0.0.1:$Port."
exit 1
