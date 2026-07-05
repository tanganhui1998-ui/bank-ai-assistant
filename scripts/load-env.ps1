param(
    [string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

# 从 .env 文件加载本地环境变量，供 Spring Boot 和 Docker Compose 复用。
if (-not (Test-Path $EnvFile)) {
    Write-Host "未找到 $EnvFile，跳过环境变量加载。"
    return
}

Get-Content -Encoding UTF8 $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) {
        return
    }
    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -le 0) {
        return
    }
    $name = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    Set-Item -Path "Env:$name" -Value $value
}

Write-Host "已加载 $EnvFile 中的本地环境变量。"
