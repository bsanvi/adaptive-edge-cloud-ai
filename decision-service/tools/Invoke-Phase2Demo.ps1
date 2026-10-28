param([string]$BaseUrl = 'http://127.0.0.1:8082', [switch]$VerifyUnconfiguredGate)
$ErrorActionPreference = 'Stop'
$modulePath = Split-Path $PSScriptRoot -Parent
$base = $BaseUrl.TrimEnd('/')
$health = Invoke-RestMethod "$base/health" -TimeoutSec 10
if ($health.status -ne 'UP') { throw 'Decision service is not healthy.' }
$body = Get-Content -LiteralPath (Join-Path $modulePath 'examples/phase2-experiments.json') -Raw
$response = Invoke-RestMethod "$base/api/v1/decision/what-if" -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 30
if ($response.source -ne 'SIMULATED' -or $response.tasksExecuted -ne $false) { throw 'Simulation labels or execution flag are incorrect.' }
$expected = Get-Content -LiteralPath (Join-Path $modulePath 'src/test/resources/contracts/accuracy-cases.json') -Raw | ConvertFrom-Json
$evaluations = @($response.baseline) + @($response.scenarios)
if ($evaluations.Count -ne $expected.Count) { throw 'Scenario count mismatch.' }
$rows = foreach ($case in $expected) {
    $evaluation = $evaluations | Where-Object { $_.name -eq $case.scenario.name } | Select-Object -First 1
    if (-not $evaluation) { throw "Missing scenario $($case.scenario.name)" }
    $actual = $evaluation.result.decision.selectedRoute
    $match = $actual -eq $case.expectedRoute
    if (-not $match) { throw "Unexpected route for $($evaluation.name): $actual" }
    [pscustomobject]@{
        Scenario = $evaluation.name
        Source = $evaluation.source
        Expected = $(if ($null -eq $case.expectedRoute) { 'NONE' } else { $case.expectedRoute })
        Observed = $(if ($null -eq $actual) { 'NONE' } else { $actual })
        Status = $evaluation.result.decision.status
        Match = $match
    }
}
$outputPath = Join-Path $modulePath 'target/demo'
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$response | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath (Join-Path $outputPath 'phase2-response.json') -Encoding UTF8
$rows | Export-Csv -LiteralPath (Join-Path $outputPath 'phase2-summary.csv') -NoTypeInformation -Encoding UTF8
$rows | Format-Table -AutoSize
Write-Host "$($rows.Count)/$($rows.Count) expected routes matched. SIMULATED inputs; no tasks executed."
Write-Host "Evidence: $outputPath"

# Invalid input must return 400 before the unavailable live-provider gate.
$invalid = $expected[0].scenario.request | ConvertTo-Json -Depth 30
$invalid = $invalid.Replace('BALANCED','UNKNOWN_PROFILE')
$status = 0
try {
    Invoke-RestMethod "$base/api/v1/decision/evaluate" -Method Post -ContentType 'application/json' -Body $invalid -TimeoutSec 10 | Out-Null
} catch {
    if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    else { throw }
}
if ($status -ne 400) { throw "Expected HTTP 400 for invalid profile; got $status" }
Write-Host 'Verified HTTP 400 for invalid priority profile.'

if ($VerifyUnconfiguredGate) {
    $valid = $expected[0].scenario.request | ConvertTo-Json -Depth 30
    foreach ($endpoint in @('evaluate','evaluate/detailed')) {
        $status = 0
        try {
            Invoke-RestMethod "$base/api/v1/decision/$endpoint" -Method Post -ContentType 'application/json' -Body $valid -TimeoutSec 10 | Out-Null
        } catch {
            if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
            else { throw }
        }
        if ($status -ne 503) { throw "Expected unconfigured HTTP 503 from $endpoint; got $status" }
        Write-Host "Verified controlled HTTP 503 from $endpoint with live providers unconfigured."
    }
}
