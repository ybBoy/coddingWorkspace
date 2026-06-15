@echo off
setlocal

set PROJECT_DIR=%~dp0
set OUT_DIR=%PROJECT_DIR%out
set PORT=8080

if "%~1" neq "" set PORT=%~1

echo ========================================
echo   二手物品整理清单 - 启动服务
echo ========================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到 java，请确保已安装 JDK 8 或 JRE 8 并配置了环境变量。
    pause
    exit /b 1
)

if not exist "%OUT_DIR%\start\DeclutterServer.class" (
    echo [错误] 未找到编译后的类文件。
    echo 请先执行 build.bat 进行编译。
    pause
    exit /b 1
)

cd /d "%PROJECT_DIR%"

echo 正在启动服务器...
echo   端口: %PORT%
echo   访问: http://localhost:%PORT%/
echo.
echo 按 Ctrl+C 停止服务器。
echo ========================================
echo.

java -cp "%OUT_DIR%" start.DeclutterServer %PORT%

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [提示] 如果端口 %PORT% 被占用，可以尝试指定其他端口：
    echo   run.bat 8081
    pause
)

endlocal
