@echo off
chcp 65001 >nul
title 保税维修工作台 - 启动
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1"
pause
