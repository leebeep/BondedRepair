@echo off
chcp 65001 >nul
title 保税维修工作台 - 停止
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1"
pause
