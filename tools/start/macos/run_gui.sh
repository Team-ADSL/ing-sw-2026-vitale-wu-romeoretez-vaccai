#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

JAR="target/mesos.jar"
SOCKET_PORT=8080
RMI_PORT=1099
SAVE_DIR="./saved"

echo "=== MESOS — GUI Session ==="
echo

echo "[1/3] Building..."
mvn package -DskipTests
if [ $? -ne 0 ]; then
  echo
  echo "BUILD FAILED. Aborting."
  exit 1
fi

if [ ! -f "$JAR" ]; then
  echo
  echo "JAR not found after build. Aborting."
  exit 1
fi

echo
echo "[2/3] Starting server (socket=$SOCKET_PORT, rmi=$RMI_PORT)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && source .env && java -jar $JAR --server $SOCKET_PORT $RMI_PORT $SAVE_DIR\""

echo "Waiting for server to start on port $SOCKET_PORT..."
until nc -z 127.0.0.1 "$SOCKET_PORT" 2>/dev/null; do
  sleep 1
done
echo "Server is up."

echo
echo "[3/3] Opening GUI clients..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $JAR --client --socket --gui 127.0.0.1 $SOCKET_PORT\""
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $JAR --client --rmi   --gui 127.0.0.1 $RMI_PORT\""

echo
echo "Done."
