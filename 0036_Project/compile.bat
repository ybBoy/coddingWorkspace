@echo off
echo ========================================
echo  编译菜谱收藏夹后端服务
echo ========================================

setlocal

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
javac -encoding UTF-8 -d "%BUILD_DIR%" -cp "%LIB_DIR%\json-20210307.jar" "%SRC_DIR%\*.java" "%SRC_DIR%\model\*.java" "%SRC_DIR%\store\*.java" "%SRC_DIR%\service\*.java" "%SRC_DIR%\controller\*.java"

if %ERRORLEVEL% EQU 0 (
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
