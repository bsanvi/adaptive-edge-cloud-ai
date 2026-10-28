param(
    [ValidateSet('Test','Run','Package','MockIntegration','Experiments')]
    [string]$Action = 'Test'
)
$ErrorActionPreference = 'Stop'
$modulePath = Split-Path $PSScriptRoot -Parent
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($mavenCommand) {
    $mavenPath = $mavenCommand.Source
} else {
    $cachedMaven = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.m2/wrapper/dists') -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $cachedMaven) { throw 'Install Maven or repair the shared wrapper; no cached Maven executable was found.' }
    $mavenPath = $cachedMaven.FullName
}
$mavenArguments = @("-Dmaven.repo.local=$env:USERPROFILE/.m2/repository", '-f', (Join-Path $modulePath 'pom.xml'))
switch ($Action) {
    'Test' { $mavenArguments += @('clean','test') }
    'Run' { $mavenArguments += 'spring-boot:run' }
    'Package' { $mavenArguments += 'package' }
    'MockIntegration' { $mavenArguments += @('-Dtest=MockTeamIntegrationTests','test') }
    'Experiments' { $mavenArguments += @('-Dtest=DecisionAccuracyExperimentsTests','test') }
}
Write-Host "Using $mavenPath ($Action)"
& $mavenPath @mavenArguments
if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
