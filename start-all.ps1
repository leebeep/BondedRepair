# ============================================================
# 保税维修工作台 一键启动脚本
# 顺序: Docker 守护进程 -> MySQL/Redis 容器 -> 后端(8080) -> 前端(80)
# ============================================================
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$logDir = Join-Path $root 'logs'
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }

function Wait-Port($port, $seconds, $name) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-NetConnection localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue) { return $true }
        Start-Sleep 3
    }
    Write-Host "[X] $name 端口 $port 等待超时" -ForegroundColor Red
    return $false
}

# ---------- 1. Docker 守护进程 ----------
Write-Host '==> [1/4] 检查 Docker 守护进程...' -ForegroundColor Cyan
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host '    Docker 未运行，尝试启动 Docker Desktop...'
    $dd = 'D:\ProgramData\Docker\resources\Docker Desktop.exe'
    $backend = 'D:\ProgramData\Docker\resources\com.docker.backend.exe'
    Start-Process $dd -ErrorAction SilentlyContinue
    Start-Sleep 10
    docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        # 兜底：直接启动后端进程（绕开 URL-handler 启动缺陷）
        Write-Host '    通过 com.docker.backend.exe 启动...'
        Start-Process $backend -ErrorAction SilentlyContinue
    }
    $ok = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep 5
        docker info *> $null
        if ($LASTEXITCODE -eq 0) { $ok = $true; break }
    }
    if (-not $ok) { Write-Host '[X] Docker 守护进程启动失败' -ForegroundColor Red; exit 1 }
}
Write-Host '    Docker 守护进程 OK' -ForegroundColor Green

# ---------- 2. MySQL / Redis 容器 ----------
Write-Host '==> [2/4] 启动 MySQL(3307)/Redis(6380) 容器...' -ForegroundColor Cyan
Push-Location $root
docker compose up -d 2>&1 | Out-Null
Pop-Location
$deadline = (Get-Date).AddSeconds(120)
$healthy = 0
do {
    Start-Sleep 5
    $healthy = (docker ps --filter "name=br-" --format "{{.Names}} {{.Status}}" | Select-String "healthy" -SimpleMatch).Count
} while ($healthy -lt 2 -and (Get-Date) -lt $deadline)
if ($healthy -lt 2) { Write-Host '[X] 容器未就绪' -ForegroundColor Red; exit 1 }
Write-Host '    MySQL / Redis healthy' -ForegroundColor Green

# ---------- 3. 后端 ----------
Write-Host '==> [3/4] 启动后端 ruoyi-admin (8080)...' -ForegroundColor Cyan
$backendUp = Test-NetConnection localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $backendUp) {
    $jar = Join-Path $root 'ruoyi-backend\ruoyi-admin\target\ruoyi-admin.jar'
    if (-not (Test-Path $jar)) {
        Write-Host '    jar 不存在，执行 Maven 打包（首次较慢）...'
        Push-Location (Join-Path $root 'ruoyi-backend')
        mvn -DskipTests -q package 2>&1 | Out-Null
        Pop-Location
        if ($LASTEXITCODE -ne 0) { Write-Host '[X] Maven 打包失败' -ForegroundColor Red; exit 1 }
    }
    Start-Process java -ArgumentList "-jar", "`"$jar`"" `
        -WorkingDirectory (Join-Path $root 'ruoyi-backend') `
        -RedirectStandardOutput (Join-Path $logDir 'backend.log') `
        -RedirectStandardError (Join-Path $logDir 'backend.err.log') `
        -WindowStyle Hidden
    if (-not (Wait-Port 8080 180 '后端')) { exit 1 }
}
Write-Host '    后端 OK: http://localhost:8080' -ForegroundColor Green

# ---------- 4. 前端 ----------
Write-Host '==> [4/4] 启动前端 ruoyi-frontend (80)...' -ForegroundColor Cyan
$frontendUp = Test-NetConnection localhost -Port 80 -InformationLevel Quiet -WarningAction SilentlyContinue
if (-not $frontendUp) {
    if (-not (Test-Path (Join-Path $root 'ruoyi-frontend\node_modules'))) {
        Write-Host '    安装前端依赖（使用 npmmirror 源）...'
        Push-Location (Join-Path $root 'ruoyi-frontend')
        npm install --registry=https://registry.npmmirror.com 2>&1 | Out-Null
        Pop-Location
    }
    Start-Process "$env:ComSpec" -ArgumentList '/c', 'npm run dev' `
        -WorkingDirectory (Join-Path $root 'ruoyi-frontend') `
        -RedirectStandardOutput (Join-Path $logDir 'frontend.log') `
        -RedirectStandardError (Join-Path $logDir 'frontend.err.log') `
        -WindowStyle Hidden
    if (-not (Wait-Port 80 120 '前端')) { exit 1 }
}
Write-Host '    前端 OK: http://localhost:80' -ForegroundColor Green

Write-Host ''
Write-Host '================ 保税维修工作台已启动 ================' -ForegroundColor Yellow
Write-Host '  访问地址: http://localhost:80   账号: admin / admin123' -ForegroundColor Yellow
Write-Host '  后端接口: http://localhost:8080' -ForegroundColor Yellow
Write-Host '  日志目录: logs\  (backend.log / frontend.log)' -ForegroundColor Yellow
Write-Host '======================================================' -ForegroundColor Yellow
