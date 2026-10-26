param(
    [int]$Port = 6379
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$memuraiHome = 'C:\Users\19097\tools\memurai\4.1.2\Memurai'
$server = Join-Path $memuraiHome 'memurai.exe'
$client = Join-Path $memuraiHome 'memurai-cli.exe'
$config = Join-Path $PSScriptRoot 'memurai.conf'
$runtimeDir = Join-Path $projectRoot 'runtime\redis'
$dataDir = Join-Path $runtimeDir 'data'
$logFile = Join-Path $runtimeDir 'memurai.log'
$pidFile = Join-Path $runtimeDir 'memurai.pid'

if (-not (Test-Path $server)) {
    throw "Memurai is not installed at $server"
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

$savedErrorAction = $ErrorActionPreference
$ErrorActionPreference = 'SilentlyContinue'
$existingPong = & $client -p $Port PING 2>$null
$existingExitCode = $LASTEXITCODE
$ErrorActionPreference = $savedErrorAction
if ($existingExitCode -eq 0 -and $existingPong -eq 'PONG') {
    Write-Host "Redis-compatible server is already running on port $Port."
    exit 0
}

$startArgs = @($config, '--dir', $dataDir, '--logfile', $logFile)
$process = Start-Process -FilePath $server -ArgumentList $startArgs -WorkingDirectory $dataDir -WindowStyle Hidden -PassThru

Set-Content -Path $pidFile -Value $process.Id -Encoding ascii

$deadline = (Get-Date).AddSeconds(20)
do {
    Start-Sleep -Milliseconds 300
    $savedErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    $pong = & $client -p $Port PING 2>$null
    $ErrorActionPreference = $savedErrorAction
    if ($pong -eq 'PONG') {
        Write-Host "Redis-compatible server is ready on 127.0.0.1:$Port (PID $($process.Id))."
        exit 0
    }
} while ((Get-Date) -lt $deadline)

throw "Redis-compatible server did not become ready. Check $logFile"
