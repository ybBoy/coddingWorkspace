@echo off
setlocal

set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%src\main\java
set OUT_DIR=%PROJECT_DIR%out

echo ========================================
echo   二手物品整理清单 - 编译脚本
echo ========================================
echo.

where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到 javac，请确保已安装 JDK 8 并配置了环境变量。
    echo.
    echo 可尝试以下方法：
    echo   1. 安装 JDK 8 或更高版本
    echo   2. 将 JDK 的 bin 目录添加到 PATH 环境变量
    echo   3. 或设置 JAVA_HOME 环境变量
    exit /b 1
)

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

echo 正在编译 Java 源文件...
echo.

set SOURCE_FILES=
for /r "%SRC_DIR%" %%f in (*.java) do (
    call set "SOURCE_FILES=%%SOURCE_FILES%% "%%f""
)

javac -encoding UTF-8 -source 1.8 -target 1.8 -d "%OUT_DIR%" %SOURCE_FILES%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   编译成功！输出目录: %OUT_DIR%
    echo ========================================
    echo.
    echo 如需运行，请执行 run.bat
) else (
    echo.
    echo [错误] 编译失败，请检查上方错误信息。
    exit /b 1
)

endlocal
