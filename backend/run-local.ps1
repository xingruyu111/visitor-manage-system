# 本地启动后端：先加载 backend/.env.local 中的环境变量，再执行 mvn spring-boot:run
# 用法：powershell -ExecutionPolicy Bypass -File .\run-local.ps1

$envFile = Join-Path $PSScriptRoot '.env.local'

if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 |
        Where-Object { $_ -match '^\s*[^#\s][^=]*=' } |
        ForEach-Object {
            $pair = $_ -split '=', 2
            [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
        }
    Write-Host "已加载 $envFile"
} else {
    Write-Warning "未找到 $envFile，将使用 application.yaml 中的默认值（localhost）"
}

Push-Location $PSScriptRoot
try {
    mvn spring-boot:run
} finally {
    Pop-Location
}
