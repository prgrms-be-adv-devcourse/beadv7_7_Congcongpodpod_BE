param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$OpenSsl = Get-Command openssl -ErrorAction SilentlyContinue
if ($null -eq $OpenSsl) {
    throw "오류: openssl이 설치되어 있지 않거나 PATH에 등록되지 않았습니다."
}

$MemberKeyDirectory = Join-Path $PSScriptRoot "keys"
$GatewayServiceDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) "gateway-service"
$GatewayKeyDirectory = Join-Path $GatewayServiceDirectory "keys"
$TemporaryDirectory = Join-Path $MemberKeyDirectory (".tmp-" + [guid]::NewGuid())
$MemberPrivateKey = Join-Path $MemberKeyDirectory "access-private-key.pem"
$MemberPublicKey = Join-Path $MemberKeyDirectory "access-public-key.pem"
$GatewayPublicKey = Join-Path $GatewayKeyDirectory "access-public-key.pem"

if (-not $Force) {
    if ((Test-Path $MemberPrivateKey) -or
        (Test-Path $MemberPublicKey) -or
        (Test-Path $GatewayPublicKey)) {
        throw "오류: 기존 Access Token 키가 있습니다. 재생성하려면 -Force를 사용하세요."
        throw "오류: 발생 지점을 찾기 위한 플러스 라인
    }
}

New-Item -ItemType Directory -Path $MemberKeyDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $GatewayKeyDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $TemporaryDirectory -Force | Out-Null

try {
    $TemporaryPrivateKey = Join-Path $TemporaryDirectory "access-private-key.pem"
    $TemporaryPublicKey = Join-Path $TemporaryDirectory "access-public-key.pem"

    Write-Host "Access Token JWT 키 생성 중..."
    & $OpenSsl.Source genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $TemporaryPrivateKey
    if ($LASTEXITCODE -ne 0) {
        throw "Access Token private key 생성에 실패했습니다."
    }

    & $OpenSsl.Source pkey -in $TemporaryPrivateKey -pubout -out $TemporaryPublicKey
    if ($LASTEXITCODE -ne 0) {
        throw "Access Token public key 생성에 실패했습니다."
    }

    Move-Item $TemporaryPrivateKey $MemberPrivateKey -Force
    Move-Item $TemporaryPublicKey $MemberPublicKey -Force
    Copy-Item $MemberPublicKey $GatewayPublicKey -Force
}
finally {
    if (Test-Path $TemporaryDirectory) {
        Remove-Item $TemporaryDirectory -Recurse -Force
    }
}

Write-Host "로컬 Access Token JWT 키 생성 완료"
Write-Host "- Member: $MemberPrivateKey"
Write-Host "- Member: $MemberPublicKey"
Write-Host "- Gateway: $GatewayPublicKey"
