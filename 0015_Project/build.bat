@echo off
chcp 65001 >nul
echo 正在编译会议室预订系统...

if not exist "backend\bin" mkdir backend\bin

javac -encoding UTF-8 -d backend\bin ^
    backend\src\com\meeting\model\User.java ^
    backend\src\com\meeting\model\MeetingRoom.java ^
    backend\src\com\meeting\model\Booking.java ^
    backend\src\com\meeting\util\JsonUtil.java ^
    backend\src\com\meeting\util\DataStore.java ^
    backend\src\com\meeting\service\BookingService.java ^
    backend\src\com\meeting\MeetingServer.java

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   编译成功！
    echo   运行 run.bat 启动服务器
    echo ========================================
) else (
    echo.
    echo 编译失败，请检查错误信息。
)
pause
