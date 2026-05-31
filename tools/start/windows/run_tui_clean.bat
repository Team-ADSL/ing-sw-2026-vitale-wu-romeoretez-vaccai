@echo off
setlocal

cd /d "%~dp0\..\..\.."

set SERVER_JAR=server\target\mesos-server.jar
set CLIENT_JAR=client\target\mesos-client.jar
set SOCKET_PORT=8080
set RMI_PORT=1099
set SAVE_DIR=.\saved

echo === MESOS — TUI Session (CLEAN: wipes saved games) ===
echo.

echo [0/3] Cleaning previous session...
rem Kill any leftover server window so it can't keep games alive in memory or
rem re-save .ser files while we delete them.
taskkill /FI "WINDOWTITLE eq MESOS Server*" /T /F >nul 2>&1
rem Delete the saved-games directory (game_<id>.ser recovery files).
if exist "%SAVE_DIR%" (
    rmdir /s /q "%SAVE_DIR%"
    echo Deleted %SAVE_DIR%.
) else (
    echo No %SAVE_DIR% directory to delete.
)
echo.

echo [1/3] Building...
call mvn package -DskipTests
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Aborting.
    pause
    exit /b 1
)

if not exist "%SERVER_JAR%" (
    echo.
    echo Server JAR not found after build. Aborting.
    pause
    exit /b 1
)
if not exist "%CLIENT_JAR%" (
    echo.
    echo Client JAR not found after build. Aborting.
    pause
    exit /b 1
)

echo.
echo [2/3] Starting server (socket=%SOCKET_PORT%, rmi=%RMI_PORT%)...
start "MESOS Server" cmd /k "java -jar %SERVER_JAR% %SOCKET_PORT% %RMI_PORT% %SAVE_DIR%"

echo Waiting for server to start on port %SOCKET_PORT%...
:wait_server
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort %SOCKET_PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto wait_server
)

echo Server is up.
echo.
echo [3/3] Opening TUI clients...
start "MESOS Socket Client (TUI)" cmd /k "java -jar %CLIENT_JAR% --client --socket --tui 127.0.0.1 %SOCKET_PORT%"
start "MESOS RMI Client (TUI)"    cmd /k "java -jar %CLIENT_JAR% --client --rmi   --tui 127.0.0.1 %RMI_PORT%"

echo.
echo Done.
pause
