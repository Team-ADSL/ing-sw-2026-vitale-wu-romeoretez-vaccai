#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$PROJECT_ROOT"

SERVER_JAR="server/target/mesos-server.jar"
CLIENT_JAR="client/target/mesos-client.jar"
SOCKET_PORT=8080
RMI_PORT=1099
SAVE_DIR="./saved"

echo "=== MESOS — TUI Session ==="
echo

echo "[1/3] Building..."
mvn package -DskipTests
if [ $? -ne 0 ]; then
  echo
  echo "BUILD FAILED. Aborting."
  exit 1
fi

if [ ! -f "$SERVER_JAR" ] || [ ! -f "$CLIENT_JAR" ]; then
  echo
  echo "JAR(s) not found after build. Aborting."
  exit 1
fi

echo
echo "[2/3] Starting server (socket=$SOCKET_PORT, rmi=$RMI_PORT)..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && source .env && java -jar $SERVER_JAR $SOCKET_PORT $RMI_PORT $SAVE_DIR\""

echo "Waiting for server to start on port $SOCKET_PORT..."
until nc -z 127.0.0.1 "$SOCKET_PORT" 2>/dev/null; do
  sleep 1
done
echo "Server is up."

echo
echo "[3/3] Opening TUI clients..."
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $CLIENT_JAR --client --socket --tui 127.0.0.1 $SOCKET_PORT\""
osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_ROOT' && java -jar $CLIENT_JAR --client --rmi   --tui 127.0.0.1 $RMI_PORT\""

echo
echo "Done."
