$ErrorActionPreference = "Stop"

# 어느 디렉터리에서 호출해도 루트 compose.yaml을 사용합니다.
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $projectRoot

try {
    $command = if ($args.Count -gt 0) { $args[0] } else { "up" }
    $commandArgs = if ($args.Count -gt 1) { $args[1..($args.Count - 1)] } else { @() }

    switch ($command) {
        "down" {
            # 컨테이너와 네트워크만 제거합니다. 볼륨 데이터와 이미지는 유지됩니다.
            & docker compose down @commandArgs
            if ($LASTEXITCODE -ne 0) { throw "docker compose down failed." }
            return
        }
        "stop" {
            # 지정한 서비스만 중지합니다. 서비스명이 없으면 전체 서비스를 중지합니다.
            & docker compose stop @commandArgs
            if ($LASTEXITCODE -ne 0) { throw "docker compose stop failed." }
            return
        }
        "reset" {
            $target = if ($commandArgs.Count -gt 0) { $commandArgs[0] } else { "" }
            $allowedTargets = @("member-db", "core-db", "payment-db", "ai-db", "kafka", "redis", "elasticsearch", "all")
            if ($target -notin $allowedTargets) {
                throw "초기화 대상을 지정하세요: $($allowedTargets -join ', ')"
            }

            Write-Warning "'$target'의 로컬 데이터가 복구할 수 없게 삭제됩니다."
            $confirmation = Read-Host "계속하려면 RESET $target 을 입력하세요"
            if ($confirmation -cne "RESET $target") {
                Write-Host "초기화를 취소했습니다."
                return
            }

            switch ($target) {
                "member-db" {
                    & docker compose stop member-service
                    & docker compose up -d member-db
                    & docker compose exec -T member-db psql -v ON_ERROR_STOP=1 -U member -d postgres `
                        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'member_db' AND pid <> pg_backend_pid();" `
                        -c "DROP DATABASE IF EXISTS member_db;" `
                        -c "CREATE DATABASE member_db OWNER member;"
                    & docker compose up -d member-service
                }
                "core-db" {
                    & docker compose stop core-service
                    & docker compose up -d core-db
                    & docker compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres `
                        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'core_db' AND pid <> pg_backend_pid();" `
                        -c "DROP DATABASE IF EXISTS core_db;" `
                        -c "CREATE DATABASE core_db OWNER core;"
                    & docker compose up -d core-service
                }
                "payment-db" {
                    & docker compose stop payment-service
                    & docker compose up -d core-db database-initializer
                    & docker compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres `
                        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'payment_db' AND pid <> pg_backend_pid();" `
                        -c "DROP DATABASE IF EXISTS payment_db;" `
                        -c "CREATE DATABASE payment_db OWNER payment;"
                    & docker compose up -d payment-service
                }
                "ai-db" {
                    & docker compose stop ai-service
                    & docker compose up -d core-db database-initializer
                    & docker compose exec -T core-db psql -v ON_ERROR_STOP=1 -U core -d postgres `
                        -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'ai_db' AND pid <> pg_backend_pid();" `
                        -c "DROP DATABASE IF EXISTS ai_db;" `
                        -c "CREATE DATABASE ai_db OWNER ai;"
                    & docker compose up -d ai-service
                }
                "kafka" {
                    & docker compose stop kafka
                    & docker compose rm -f kafka
                    & docker volume rm lastdish-local_kafka-data 2>$null
                    & docker compose up -d kafka
                }
                "redis" {
                    & docker compose up -d redis
                    & docker compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli FLUSHALL'
                }
                "elasticsearch" {
                    & docker compose stop elasticsearch
                    & docker compose rm -f elasticsearch
                    & docker volume rm lastdish-local_elasticsearch-data 2>$null
                    & docker compose up -d elasticsearch
                }
                "all" {
                    # 전체 컨테이너를 제거한 뒤 모든 영속 데이터 볼륨을 삭제합니다.
                    # Redis는 비영속 구성이므로 컨테이너 제거만으로 데이터가 초기화됩니다.
                    & docker compose down
                    if ($LASTEXITCODE -ne 0) { throw "docker compose down failed." }
                    foreach ($volume in @(
                        "lastdish-local_member-db-data",
                        "lastdish-local_core-db-data",
                        "lastdish-local_kafka-data",
                        "lastdish-local_elasticsearch-data"
                    )) {
                        & docker volume inspect $volume *> $null
                        if ($LASTEXITCODE -eq 0) {
                            & docker volume rm $volume
                            if ($LASTEXITCODE -ne 0) { throw "Failed to remove Docker volume: $volume" }
                        }
                    }
                    & docker compose up -d --build
                }
            }
            if ($LASTEXITCODE -ne 0) { throw "Failed to reset: $target" }
            Write-Host "'$target' 초기화를 완료했습니다."
            return
        }
        { $_ -in "-h", "--help", "help" } {
            Write-Host "Usage: .\dev.ps1 [up] [service ...]"
            Write-Host "       .\dev.ps1 stop [service ...]"
            Write-Host "       .\dev.ps1 down"
            Write-Host "       .\dev.ps1 reset <member-db|core-db|payment-db|ai-db|kafka|redis|elasticsearch|all>"
            return
        }
        "up" {
            $services = $commandArgs
        }
        default {
            # 기존 Bash 사용법과 동일하게 첫 번째 인수를 서비스명으로 처리합니다.
            $services = @($command) + $commandArgs
        }
    }

    # 빌드 실패 시 기존 이미지를 보존하기 위해 빌드 전에 이미지 ID를 기록합니다.
    $beforeImages = @(& docker compose images -q 2>$null | Sort-Object -Unique)

    & docker compose up -d --build @services
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed." }

    # 새 컨테이너가 참조하지 않는 교체 전 이미지만 정리합니다.
    $afterImages = @(& docker compose images -q | Sort-Object -Unique)
    foreach ($imageId in $beforeImages) {
        if ($imageId -and $imageId -notin $afterImages) {
            # 다른 컨테이너가 사용 중이면 Docker가 삭제를 거부하므로 강제 삭제하지 않습니다.
            & docker image rm $imageId
        }
    }
}
finally {
    Pop-Location
}
