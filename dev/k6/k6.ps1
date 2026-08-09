param(
    [Parameter(Position = 0)]
    [string]$Scenario = "smoke",

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$K6Arguments
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ScriptDirectory = $PSScriptRoot
$EnvironmentFile = Join-Path $ScriptDirectory ".env"
$ScenarioFile = Join-Path $ScriptDirectory "$Scenario.js"

if (-not (Test-Path $EnvironmentFile)) {
    throw "dev/k6/.env가 없습니다. .env.example을 복사해 주세요."
}

Get-Content $EnvironmentFile | ForEach-Object {
    $Line = $_.Trim()
    if (($Line.Length -eq 0) -or $Line.StartsWith("#")) {
        return
    }

    $Parts = $Line.Split("=", 2)
    if ($Parts.Count -eq 2) {
        [Environment]::SetEnvironmentVariable($Parts[0].Trim(), $Parts[1].Trim(), "Process")
    }
}

if ([string]::IsNullOrWhiteSpace($env:BASE_URL)) {
    throw "dev/k6/.env에 BASE_URL을 설정하세요."
}

if (-not (Test-Path $ScenarioFile)) {
    $AvailableScenarios = Get-ChildItem $ScriptDirectory -Filter "*.js" |
        ForEach-Object { $_.BaseName } |
        Sort-Object
    throw "알 수 없는 시나리오입니다: $Scenario`n사용 가능한 시나리오: $($AvailableScenarios -join ', ')"
}

$DockerArguments = @(
    "run", "--rm", "-i",
    "-v", "${ScriptDirectory}:/scripts:ro",
    "-e", "BASE_URL=$env:BASE_URL",
    "grafana/k6", "run"
) + $K6Arguments + @("/scripts/$Scenario.js")

& docker @DockerArguments
if ($LASTEXITCODE -ne 0) {
    throw "k6 실행에 실패했습니다. 종료 코드: $LASTEXITCODE"
}
