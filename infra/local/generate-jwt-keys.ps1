param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$OpenSsl = Get-Command openssl -ErrorAction SilentlyContinue
if ($null -eq $OpenSsl) {
    throw "오류: openssl이 설치되어 있지 않거나 PATH에 등록되지 않았습니다."
}

$KeyDirectory = Join-Path $PSScriptRoot "keys"
$TemporaryDirectory = Join-Path $KeyDirectory (".tmp-" + [guid]::NewGuid())
$KeyTypes = @("access", "refresh")

if (-not $Force) {
    foreach ($KeyType in $KeyTypes) {
        $PrivateKey = Join-Path $KeyDirectory "$KeyType-private-key.pem"
        $PublicKey = Join-Path $KeyDirectory "$KeyType-public-key.pem"
        if ((Test-Path $PrivateKey) -or (Test-Path $PublicKey)) {
            throw "오류: 기존 키가 있습니다. 재생성하려면 -Force를 사용하세요."
        }
    }
}

New-Item -ItemType Directory -Path $TemporaryDirectory -Force | Out-Null

try {
    foreach ($KeyType in $KeyTypes) {
        $PrivateKey = Join-Path $TemporaryDirectory "$KeyType-private-key.pem"
        $PublicKey = Join-Path $TemporaryDirectory "$KeyType-public-key.pem"

        Write-Host "$KeyType JWT 키 생성 중..."
        & $OpenSsl.Source genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $PrivateKey
        if ($LASTEXITCODE -ne 0) {
            throw "$KeyType private key 생성에 실패했습니다."
        }

        & $OpenSsl.Source pkey -in $PrivateKey -pubout -out $PublicKey
        if ($LASTEXITCODE -ne 0) {
            throw "$KeyType public key 생성에 실패했습니다."
        }
    }

    New-Item -ItemType Directory -Path $KeyDirectory -Force | Out-Null
    foreach ($KeyType in $KeyTypes) {
        Move-Item (Join-Path $TemporaryDirectory "$KeyType-private-key.pem") $KeyDirectory -Force
        Move-Item (Join-Path $TemporaryDirectory "$KeyType-public-key.pem") $KeyDirectory -Force
    }
}
finally {
    if (Test-Path $TemporaryDirectory) {
        Remove-Item $TemporaryDirectory -Recurse -Force
    }
}

Write-Host "로컬 JWT 키 생성 완료: $KeyDirectory"
Write-Host "- Gateway: access-public-key.pem"
Write-Host "- Member: access/refresh private-key.pem"
