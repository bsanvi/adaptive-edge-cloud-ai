param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')
$services = Invoke-RestMethod "$base/api/v1/integrated/services"
$services | Format-List
$task = @{taskId=('verify-p4-' + [Guid]::NewGuid().ToString('N'));samples=@(0.1,0.2,0.3,0.2,0.1,0.4,0.1,0.3);deadlineMs=20000;privacyRequired=$true;complexity='LOW';previousRoute=$null}
$analysis = Invoke-RestMethod "$base/api/v1/integrated/analyze" -Method Post -ContentType 'application/json' -Body ($task | ConvertTo-Json) -TimeoutSec 90
if ($analysis.report.status -ne 'ANALYZED') { throw ('Analysis failed: ' + ($analysis.report | ConvertTo-Json -Depth 15)) }
if ($analysis.report.executedRoute) { throw 'Analysis unexpectedly executed inference' }
if ($analysis.report.selectedRoute -ne 'EDGE') { throw 'Privacy task did not select EDGE' }
$result = Invoke-RestMethod "$base/api/v1/integrated/analysis/$($analysis.analysisId)/execute" -Method Post -TimeoutSec 90
if ($result.status -ne 'SUCCESS' -or $result.executedRoute -ne 'EDGE') { throw ('Execution failed: ' + ($result | ConvertTo-Json -Depth 15)) }
$id = [long]$result.reportId
if ($id -le 0) { throw 'Server did not return a numeric saved reportId' }
$report = Invoke-RestMethod "$base/api/v1/integrated/report/$id"
if ($report.taskId -ne $task.taskId) { throw 'Saved report does not match the executed task' }
if (-not $report.decision.orchestrationAudit.device) { throw 'Original device snapshot missing' }
try {
    Invoke-RestMethod "$base/api/v1/integrated/analysis/$($analysis.analysisId)/execute" -Method Post -TimeoutSec 90 | Out-Null
    throw 'Duplicate execution unexpectedly succeeded'
} catch {
    if (-not $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 409) { throw }
}
Write-Host "PASS: analysis-only, privacy EDGE, single execution, H2 saved report ID $id, duplicate rejected."
Write-Host "Open $base/research/index.html#/results?id=$id"
Write-Host 'Inspect Audit, Print/PDF and Analytics. Two-laptop placement still requires the remote Cloud console check.'
