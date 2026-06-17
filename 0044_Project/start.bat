@echo off
chcp 65001 >nul
echo ========================================
echo    家庭药箱管理应用
echo ========================================
echo.
echo [1/3] 检查 Python 环境...
python --version
if errorlevel 1 (
    echo 错误: 未找到 Python，请先安装 Python 3.11.9
    pause
    exit /b 1
)
echo.
echo [2/3] 安装依赖...
python -m pip install -r requirements.txt
if errorlevel 1 (
    echo 错误: 依赖安装失败
    pause
    exit /b 1
)
echo.
echo [3/3] 启动服务...
echo.
echo 服务启动后，请在浏览器中访问: http://localhost:5000
echo 按 Ctrl+C 停止服务
echo.
python -m api.app
pause
