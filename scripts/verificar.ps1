$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
& $PSScriptRoot\iniciar.ps1 -NoBrowser
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$testProject = 'api-cuestionarios-practica-tests'
$testExitCode = 1
try {
    & docker compose -p $testProject -f compose.test.yaml up --build --abort-on-container-exit --exit-code-from backend-tests
    $testExitCode = $LASTEXITCODE
} finally {
    & docker compose -p $testProject -f compose.test.yaml down
}
if ($testExitCode -ne 0) { exit $testExitCode }
Write-Host 'Verificación completa finalizada.'
