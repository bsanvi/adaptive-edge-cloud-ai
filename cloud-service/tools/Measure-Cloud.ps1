param(
    [string]$BaseUrl = 'http://127.0.0.1:8083',
    [ValidateRange(5,1000)][int]$Trials = 30,
    [string]$OutputPath = '.tools/cloud-calibration.json'
)
$ErrorActionPreference = 'Stop'
function Median($Values) {
    $sorted = @($Values | Sort-Object)
    $middle = [int][Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2
}
$ready = Invoke-RestMethod "$BaseUrl/api/v1/cloud/ready" -TimeoutSec 5
$results = @()
foreach ($count in @(128,512,4096)) {
    $samples = @(for ($i=0; $i -lt $count; $i++) { [Math]::Sin($i * 0.1) })
    $elapsedValues = @()
    $inferenceValues = @()
    $requestBytes = 0
    for ($trial=-5; $trial -lt $Trials; $trial++) {
        $body = @{requestId=[guid]::NewGuid().ToString();taskId="calibration-$count";privacyLevel='NORMAL';samples=$samples} | ConvertTo-Json -Compress -Depth 5
        $requestBytes = [Text.Encoding]::UTF8.GetByteCount($body)
        $timer = [Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-RestMethod "$BaseUrl/api/v1/cloud/predict" -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 5
        $timer.Stop()
        if ($trial -ge 0) { $elapsedValues += $timer.Elapsed.TotalMilliseconds; $inferenceValues += $response.inferenceTimeMs }
    }
    $results += [ordered]@{sampleCount=$count;trials=$Trials;warmups=5;requestBodyBytes=$requestBytes;medianHttpMs=(Median $elapsedValues);medianInferenceMs=(Median $inferenceValues)}
}
$report = [ordered]@{measuredAt=[DateTime]::UtcNow.ToString('o');clientDevice=$env:COMPUTERNAME;serverUrl=$BaseUrl;modelVersion=$ready.modelVersion;modelSha256=$ready.modelSha256;source='MEASURED';results=$results}
$parent = Split-Path -Parent $OutputPath
if ($parent) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $OutputPath -Encoding UTF8
Write-Output "Saved raw Cloud benchmark to $OutputPath. This does not measure power, Edge timings or physical network bandwidth."
