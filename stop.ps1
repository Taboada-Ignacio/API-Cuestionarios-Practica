$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'scripts\detener.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
