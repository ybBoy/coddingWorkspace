@echo off
chcp 65001 >nul

if not exist "backend\bin\com\meeting\MeetingServer.class" (
    echo 未找到编译文件，正在自动编译...
    call build.bat
    if not exist "backend\bin\com\meeting\MeetingServer.class" (
        echo 编译失败，无法启动。
        pause
        exit /b 1
    )
)

echo ========================================
echo   会议室预订管理系统启动中...
echo   访问地址: http://localhost:8088
echo   按 Ctrl+C 停止服务器
echo ========================================
echo.

cd /d "%~dp0"
java -cp backend\bin com.meeting.MeetingServer backend\data

pause
