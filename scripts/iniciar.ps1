param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectPath
function Test-DockerReady {
    $probe = New-Object System.Diagnostics.Process
    $probe.StartInfo.FileName = (Get-Command docker).Source
    $probe.StartInfo.Arguments = 'info'
    $probe.StartInfo.UseShellExecute = $false
    $probe.StartInfo.CreateNoWindow = $true
    $probe.StartInfo.RedirectStandardOutput = $true
    $probe.StartInfo.RedirectStandardError = $true
    try {
        [void]$probe.Start()
        $outputTask = $probe.StandardOutput.ReadToEndAsync()
        $errorTask = $probe.StandardError.ReadToEndAsync()
        if (-not $probe.WaitForExit(10000)) { $probe.Kill(); $probe.WaitForExit(); return $false }
        return $probe.ExitCode -eq 0
    } finally { $probe.Dispose() }
}
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Instalá Docker Desktop y volvé a ejecutar Iniciar.cmd.'
    }
    if (-not (Test-DockerReady)) {
        $desktopPath = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
        if (-not (Test-Path -LiteralPath $desktopPath)) { throw 'Abrí Docker Desktop y volvé a iniciar.' }
        Write-Host 'Iniciando Docker Desktop...'
        Start-Process -FilePath $desktopPath -WindowStyle Hidden
        $deadline = (Get-Date).AddMinutes(3)
        do {
            Start-Sleep -Seconds 3
            if (Test-DockerReady) { break }
        } while ((Get-Date) -lt $deadline)
        if (-not (Test-DockerReady)) { throw 'Docker no pudo iniciar. Revisá Docker Desktop y que WSL 2 esté disponible.' }
    }
    if (-not (Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'La configuración de Docker no es válida.' }
    Write-Host 'Construyendo e iniciando el proyecto...'
    & docker compose up --build -d --wait --wait-timeout 300
    if ($LASTEXITCODE -ne 0) {
        & docker compose logs --tail 80
        throw 'No se pudieron iniciar todos los servicios. Revisá los mensajes anteriores.'
    }
    $port = '8090'
    foreach ($line in Get-Content -LiteralPath '.env') {
        if ($line -match '^\s*APP_PORT\s*=\s*(\d+)\s*$') { $port = $Matches[1] }
    }
    if ($env:APP_PORT) { $port = $env:APP_PORT }
    $url = "http://127.0.0.1:$port/"
    Write-Host "Proyecto listo: $url"
    & docker compose ps
    if (-not $NoBrowser) { Start-Process $url }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
