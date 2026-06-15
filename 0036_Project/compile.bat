@echo off
echo ========================================
echo  编译菜谱收藏夹后端服务
echo ========================================

setlocal enabledelayedexpansion

set "SRC_DIR=server\src\main\java"
set "BUILD_DIR=server\build\classes"
set "LIB_DIR=server\lib"

echo.
echo [1/3] 创建编译输出目录...
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

echo.
echo [2/3] 清理旧的编译文件...
for /f "delims=" %%f in ('dir /s /b "%BUILD_DIR%\*.class" 2^>nul') do del "%%f"

echo.
echo [3/3] 编译 Java 源代码...
pushd "%SRC_DIR%"
set "JAVA_FILES="
for /r %%f in (*.java) do (
    set "JAVA_FILES=!JAVA_FILES! "%%~pnxf""
)
javac -encoding UTF-8 -d "..\..\..\build\classes" -cp "..\..\..\lib\json-20210307.jar" %JAVA_FILES%
set "ERR_LEVEL=%ERRORLEVEL%"
popd

if %ERR_LEVEL% EQU 0 (
    echo.
    echo ========================================
    echo  ✅ 编译成功！
    echo ========================================
    echo.
    echo 运行 run.bat 启动服务器
    echo.
) else (
    echo.
    echo ========================================
    echo  ❌ 编译失败，请检查错误信息
    echo ========================================
    echo.
)

endlocal
pause
