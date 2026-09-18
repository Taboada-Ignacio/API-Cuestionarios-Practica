$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
& docker compose down
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Proyecto detenido. Los cuestionarios y resultados quedan conservados.'
