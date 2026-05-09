@echo off
setlocal

cd /d "%~dp0\..\..\.."

set JAR=target\mesos.jar
set SOCKET_PORT=8080
set RMI_PORT=1099
set SAVE_DIR=.\saved

echo === MESOS — GUI Session ===
echo.

echo [1/3] Building...
call mvn package -DskipTests
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Aborting.
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo.
    echo JAR not found after build. Aborting.
    pause
    exit /b 1
)

echo.
echo [2/3] Starting server (socket=%SOCKET_PORT%, rmi=%RMI_PORT%)...
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
echo [3/3] Opening GUI clients...
start "MESOS Socket Client (GUI)" cmd /k "java -jar %JAR% --client --socket --gui 127.0.0.1 %SOCKET_PORT%"
start "MESOS RMI Client (GUI)"    cmd /k "java -jar %JAR% --client --rmi   --gui 127.0.0.1 %RMI_PORT%"

echo.
echo Done.
pause
