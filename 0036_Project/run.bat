@echo off
echo ========================================
echo  启动菜谱收藏夹后端服务
echo ========================================
echo.

setlocal

set "BUILD_DIR=server\build\classes"
set "LIB_DIR=server\lib"

if not exist "%BUILD_DIR%\AppServer.class" (
    echo ❌ 找不到编译后的类文件，请先运行 compile.bat 进行编译
    echo.
    pause
    exit /b 1
)

echo 服务器正在启动...
echo 访问地址: http://localhost:8080
echo 按 Ctrl+C 停止服务器
echo.
echo ========================================
echo.

java -cp "%BUILD_DIR%;%LIB_DIR%\json-20210307.jar" AppServer

endlocal
