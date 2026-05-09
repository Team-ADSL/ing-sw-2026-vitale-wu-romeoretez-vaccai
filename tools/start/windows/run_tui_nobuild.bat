@echo off
setlocal

cd /d "%~dp0\..\..\.."

set JAR=target\mesos.jar
set SOCKET_PORT=8080
set RMI_PORT=1099
set SAVE_DIR=.\saved

echo === MESOS — TUI Session (no build) ===
echo.

if not exist "%JAR%" (
    echo JAR not found: %JAR%. Run run_tui.bat first to build.
    pause
    exit /b 1
)

echo [1/2] Starting server (socket=%SOCKET_PORT%, rmi=%RMI_PORT%)...
start "MESOS Server" cmd /k "java -jar %JAR% --server %SOCKET_PORT% %RMI_PORT% %SAVE_DIR%"

echo Waiting for server to start on port %SOCKET_PORT%...
:wait_server
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort %SOCKET_PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto wait_server
)

echo Server is up.
echo.
echo [2/2] Opening TUI clients...
start "MESOS Socket Client (TUI)" cmd /k "java -jar %JAR% --client --socket --tui 127.0.0.1 %SOCKET_PORT%"
start "MESOS RMI Client (TUI)"    cmd /k "java -jar %JAR% --client --rmi   --tui 127.0.0.1 %RMI_PORT%"

echo.
echo Done.
pause
