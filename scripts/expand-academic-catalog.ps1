param(
    [string]$CatalogPath = "$PSScriptRoot/../src/main/resources/academic-catalog.tsv"
)

$ErrorActionPreference = 'Stop'
$baseUrl = 'https://guiadecarreras.siu.edu.ar/ciie_ofertas/2.0/guia_grado.php'
$durations = @{}

function Normalize-Cell([string]$html) {
    $withoutTags = [regex]::Replace($html, '<[^>]+>', ' ')
    $decoded = [System.Net.WebUtility]::HtmlDecode($withoutTags)
    return ([regex]::Replace($decoded, '\s+', ' ')).Trim()
}

function Convert-ToYears([string]$duration) {
    if ($duration -notmatch '(?<amount>\d+(?:[\.,]\d+)?)\s*(?<unit>Años?|Semestres?|Cuatrimestres?|Meses?)') {
        return $null
    }
    $amount = [double]::Parse($Matches.amount.Replace(',', '.'), [System.Globalization.CultureInfo]::InvariantCulture)
    $years = switch -Regex ($Matches.unit) {
        '^Año' { $amount; break }
        '^Semestre' { $amount / 2; break }
        '^Cuatrimestre' { $amount / 2; break }
        '^Mes' { $amount / 12; break }
    }
    return [int][Math]::Ceiling($years)
}

foreach ($regime in @('PU', 'PR')) {
    foreach ($modality in @('0', '1')) {
        $query = 'ah=st6357bfb75e871&ai=ciie_ofertas%7C%7C14000101&disciplina=&idtitulopresencial={0}&institucion=nopar&localidad=&nivel=1&provincia=nopar&rama=nopar&regimen={1}&tcm=popup&titulo=&tm=1' -f $modality, $regime
        $html = & curl.exe -fsSL --max-time 60 "$baseUrl`?$query"
        if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar SIU ($regime/$modality)" }
        $html = $html -join "`n"
        foreach ($row in [regex]::Matches($html, '<tr[^>]*>(?<body>.*?)</tr>', 'Singleline,IgnoreCase')) {
            $cells = [regex]::Matches($row.Groups['body'].Value, '<td[^>]*>(?<body>.*?)</td>', 'Singleline,IgnoreCase')
            if ($cells.Count -lt 5) { continue }
            $university = Normalize-Cell $cells[0].Groups['body'].Value
            $faculty = Normalize-Cell $cells[1].Groups['body'].Value
            $career = Normalize-Cell $cells[2].Groups['body'].Value
            $years = Convert-ToYears (Normalize-Cell $cells[4].Groups['body'].Value)
            if (!$university -or !$faculty -or !$career -or !$years) { continue }
            $key = "$university`t$faculty`t$career"
            if (!$durations.ContainsKey($key) -or $years -gt $durations[$key]) { $durations[$key] = $years }
        }
    }
}

$rows = Get-Content -LiteralPath $CatalogPath -Encoding UTF8
$universityNames = @{}
$facultyNames = @{}
$updated = [System.Collections.Generic.List[string]]::new()
$updated.Add("type`tid`tparent`tname`tdurationYears")
$matched = 0

foreach ($line in $rows | Select-Object -Skip 1) {
    $parts = [regex]::Split($line, "`t")
    if ($parts.Count -lt 4) { throw "Fila inválida en el catálogo: $line" }
    switch ($parts[0]) {
        'U' { $universityNames[$parts[1]] = $parts[3]; $updated.Add("$line`t-") }
        'F' { $facultyNames[$parts[1]] = @($parts[2], $parts[3]); $updated.Add("$line`t-") }
        'C' {
            $faculty = $facultyNames[$parts[2]]
            $key = "$($universityNames[$faculty[0]])`t$($faculty[1])`t$($parts[3])"
            $years = if ($durations.ContainsKey($key)) { $matched++; $durations[$key] } else { '-' }
            $updated.Add("$line`t$years")
        }
        default { throw "Tipo inválido en el catálogo: $($parts[0])" }
    }
}

[System.IO.File]::WriteAllLines((Resolve-Path -LiteralPath $CatalogPath), $updated, [System.Text.UTF8Encoding]::new($false))
Write-Output "Ofertas con duración: $($durations.Count); carreras del catálogo ampliadas: $matched"
