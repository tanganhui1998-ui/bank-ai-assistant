param(
    [string]$Profile = "local"
)

$ErrorActionPreference = "Stop"

# 启动前自动读取 .env，避免每次手工设置 MySQL、Redis、Qwen、OSS 等环境变量。
.\scripts\load-env.ps1

# Reuse build.ps1 so local startup gets the same Java/Maven selection.
.\scripts\build.ps1 -SkipTests

$env:SPRING_PROFILES_ACTIVE = $Profile
& mvn spring-boot:run
