$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ScriptDirectory = $PSScriptRoot
$EnvironmentFile = Join-Path $ScriptDirectory ".env"
$TunnelProcess = $null
$KibanaStarted = $false

if (-not (Test-Path $EnvironmentFile)) {
    throw "$EnvironmentFile 파일이 없습니다. dev/kibana/.env.example을 .env로 복사한 뒤 실제 토큰을 입력하세요."
}

# 토큰 누락·빈 값은 SSH 연결 전에 Compose 설정 검증 단계에서 차단합니다.
& docker compose --env-file $EnvironmentFile --file (Join-Path $ScriptDirectory "compose.yaml") config --quiet
if ($LASTEXITCODE -ne 0) {
    throw "Kibana Compose 환경변수 검증에 실패했습니다. .env의 토큰을 확인하세요."
}

try {
    Write-Host "Data EC2 Elasticsearch SSH 터널을 여는 중..."
    $TunnelProcess = Start-Process -FilePath "ssh" -ArgumentList @(
        "-NT",
        "-o", "ExitOnForwardFailure=yes",
        "-o", "ServerAliveInterval=60",
        "-L", "127.0.0.1:19200:10.30.2.93:9200",
        "lastdish-data"
    ) -PassThru

    $TunnelReady = $false
    for ($Attempt = 0; $Attempt -lt 20; $Attempt++) {
        if ($TunnelProcess.HasExited) {
            throw "SSH 터널 프로세스가 종료됐습니다. SSH 설정과 개인키를 확인하세요."
        }

        $Connection = Test-NetConnection 127.0.0.1 -Port 19200 -WarningAction SilentlyContinue
        if ($Connection.TcpTestSucceeded) {
            $TunnelReady = $true
            break
        }
        Start-Sleep -Milliseconds 250
    }

    if (-not $TunnelReady) {
        throw "localhost:19200 SSH 터널을 확인할 수 없습니다."
    }

    Write-Host "Kibana 컨테이너를 시작하는 중..."
    & docker compose --env-file $EnvironmentFile --file (Join-Path $ScriptDirectory "compose.yaml") up --detach
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose 실행에 실패했습니다."
    }
    $KibanaStarted = $true

    Write-Host "Kibana 준비 상태를 확인하는 중..."
    $KibanaReady = $false
    for ($Attempt = 0; $Attempt -lt 120; $Attempt++) {
        try {
            $Response = Invoke-WebRequest -Uri "http://127.0.0.1:5601/api/status" -UseBasicParsing -TimeoutSec 2
            if ($Response.StatusCode -eq 200) {
                $KibanaReady = $true
                break
            }
        }
        catch {
            # 시작 중의 연결 실패와 503 응답은 다음 확인까지 기다립니다.
        }

        if ($TunnelProcess.HasExited) {
            throw "SSH 터널 프로세스가 종료됐습니다."
        }
        Start-Sleep -Seconds 1
    }

    if (-not $KibanaReady) {
        & docker compose --env-file $EnvironmentFile --file (Join-Path $ScriptDirectory "compose.yaml") logs --tail 80 kibana
        throw "120초 안에 Kibana가 준비되지 않았습니다."
    }

    Write-Host "Kibana 실행 완료: http://localhost:5601"
    $OpenBrowser = Read-Host "브라우저를 자동으로 열까요? [y/N]"
    if ($OpenBrowser -match '^(y|yes)$') {
        Start-Process "http://localhost:5601"
    }
    else {
        Write-Host "브라우저에서 http://localhost:5601 을 직접 여세요."
    }
    Write-Host "Kibana와 SSH 터널을 종료하려면 Ctrl+C를 누르세요."

    while (-not $TunnelProcess.HasExited) {
        Start-Sleep -Seconds 1
    }
    throw "SSH 터널이 종료됐습니다."
}
finally {
    if ($KibanaStarted) {
        & docker compose --env-file $EnvironmentFile --file (Join-Path $ScriptDirectory "compose.yaml") down *> $null
    }
    if (($null -ne $TunnelProcess) -and (-not $TunnelProcess.HasExited)) {
        Stop-Process -Id $TunnelProcess.Id -Force
    }
}
