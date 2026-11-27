# ============================================================
# 保税维修工作台 一键关闭脚本
# 顺序: 前端 -> 后端 -> MySQL/Redis 容器(docker compose stop)
# 数据卷保留，重新启动不丢数据
# ============================================================
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# ---------- 1. 前端 (vite/node，仅匹配本项目目录) ----------
Write-Host '==> [1/3] 停止前端...' -ForegroundColor Cyan
Get-CimInstance Win32_Process -Filter "Name='node.exe'" |
    Where-Object { $_.CommandLine -like '*ruoyi-frontend*' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue; "    stopped node pid=$($_.ProcessId)" }

# ---------- 2. 后端 ----------
Write-Host '==> [2/3] 停止后端 ruoyi-admin...' -ForegroundColor Cyan
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like '*ruoyi-admin*' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue; "    stopped java pid=$($_.ProcessId)" }

# ---------- 3. 容器 ----------
Write-Host '==> [3/3] 停止 MySQL/Redis 容器 (数据卷保留)...' -ForegroundColor Cyan
Push-Location $root
docker compose stop 2>&1 | Out-Null
Pop-Location

Write-Host ''
Write-Host '================ 保税维修工作台已关闭 ================' -ForegroundColor Yellow
Write-Host '  数据已保留。重新启动请运行 start-all.ps1' -ForegroundColor Yellow
Write-Host '======================================================' -ForegroundColor Yellow
