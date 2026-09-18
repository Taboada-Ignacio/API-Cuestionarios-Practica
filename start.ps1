param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'scripts\iniciar.ps1') -NoBrowser:$NoBrowser
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
