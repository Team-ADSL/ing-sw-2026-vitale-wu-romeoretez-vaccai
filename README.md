# MESOS — Software Engineering Project 2026

**Team:** Vitale · Wu · Romeo-Retez · Vaccai

---

## Prerequisites

- Java 17+
- Maven 3.8+ (to build)
- Python 3.8+ (for testers)

---

## Build

```bash
mvn package
```

Add `-DskipTests` to skip unit tests and speed up the build:

```bash
mvn package -DskipTests
```

The compiled jar will be at `target/mesos.jar`.

---

## Running the Application

### 1. Start the Server

Open a terminal and run:

```bash
java -jar target/mesos.jar --server <socket-port> <rmi-port> <recover-directory>
```

| Argument             | Description                                              |
|----------------------|----------------------------------------------------------|
| `<socket-port>`      | Port for Socket connections (e.g. `8080`)               |
| `<rmi-port>`         | Port for RMI registry (e.g. `1099`)                     |
| `<recover-directory>`| Directory for game persistence files (e.g. `./saved`)  |

**Example:**

```bash
java -jar target/mesos.jar --server 8080 1099 ./saved
```

> The two ports must be different and in the range 1024–65535.
> The server starts both a Socket listener and an RMI registry.

---

### 2. Start a Socket Client

Open a **new terminal** and run:

```bash
java -jar target/mesos.jar --client --socket --tui <server-ip> <socket-port>
```

**Example (local machine):**

```bash
java -jar target/mesos.jar --client --socket --tui 127.0.0.1 8080
```

---

### 3. Start an RMI Client

Open a **new terminal** and run:

```bash
java -jar target/mesos.jar --client --rmi --tui <server-ip> <rmi-port>
```

**Example (local machine):**

```bash
java -jar target/mesos.jar --client --rmi --tui 127.0.0.1 1099
```

---

### 4. Start a GUI Client (JavaFX)

The GUI client uses JavaFX 21 and is bundled inside the fat jar
(`win`, `linux`, `mac`, `mac-aarch64` natives are all included).

Over Socket:

```bash
java -jar target/mesos.jar --client --socket --gui <server-ip> <socket-port>
```

Over RMI:

```bash
java -jar target/mesos.jar --client --rmi --gui <server-ip> <rmi-port>
```

**Examples (local machine):**

```bash
java -jar target/mesos.jar --client --socket --gui 127.0.0.1 8080
```
```bash
java -jar target/mesos.jar --client --rmi    --gui 127.0.0.1 1099
```

> Java 25 may print warnings about native access and `sun.misc.Unsafe`; they
> come from JavaFX itself and can be silenced by appending
> `--enable-native-access=ALL-UNNAMED` (cosmetic only — the jar runs fine
> without it).

---

### Client Options Reference

```
java -jar target/mesos.jar --client <connection> <interface> <server-ip> <port>
```

| Argument       | Options              | Description                         |
|----------------|----------------------|-------------------------------------|
| `<connection>` | `--socket` / `--rmi` | Network protocol to use             |
| `<interface>`  | `--tui` / `--gui`    | UI mode (TUI = terminal, GUI = JavaFX) |
| `<server-ip>`  | e.g. `127.0.0.1`     | IP address of the server            |
| `<port>`       | e.g. `8080` / `1099` | Port matching the chosen protocol   |

---

## Typical Local Session

Terminal 1 (server):
```
  java -jar target/mesos.jar --server 8080 1099 ./saved
```
Terminal 2 (socket client, TUI):

```
  java -jar target/mesos.jar --client --socket --tui 127.0.0.1 8080
```
Terminal 3 (rmi client, TUI):

```
  java -jar target/mesos.jar --client --rmi --tui 127.0.0.1 1099
```
Terminal 4 (socket client, GUI):

```
  java -jar target/mesos.jar --client --socket --gui 127.0.0.1 8080
```

---

## Quick Start Scripts (`tools/start`)

The `tools/start/` folder contains scripts that automate launching a full local session — server + two clients (one Socket, one RMI) — with a single command from the IntelliJ terminal.

There are four scripts per platform:

| Script | Build | Interface |
|--------|-------|-----------|
| `run_tui`        | yes | TUI (terminal) |
| `run_gui`        | yes | GUI (JavaFX)   |
| `run_tui_nobuild`| no  | TUI (terminal) |
| `run_gui_nobuild`| no  | GUI (JavaFX)   |

The `_nobuild` variants skip the Maven build step and fail immediately if `target/mesos.jar` is not found.

### macOS

**From IntelliJ:** right-click the script in the Project panel → **Run**.

Alternatively, from the IntelliJ terminal:

```bash
./tools/start/macos/run_tui.sh          # build + TUI session
```
```bash
./tools/start/macos/run_gui.sh          # build + GUI session
```
```bash
./tools/start/macos/run_tui_nobuild.sh  # TUI session (skip build)
```
```bash
./tools/start/macos/run_gui_nobuild.sh  # GUI session (skip build)
```

Each script builds the project (unless `_nobuild`), starts the server in a new Terminal window, waits for it to be ready on port 8080, then opens two client windows.

### Windows

**From IntelliJ:** right-click the script in the Project panel → **Run**.

Alternatively, from the IntelliJ terminal:

```
tools\start\windows\run_tui.bat          # build + TUI session
```
```
tools\start\windows\run_gui.bat          # build + GUI session
```
```
tools\start\windows\run_tui_nobuild.bat  # TUI session (skip build)
```
```
tools\start\windows\run_gui_nobuild.bat  # GUI session (skip build)
```

Each script builds the project (unless `_nobuild`), opens a new `cmd` window for the server, waits for it to be ready on port 8080, then opens two more `cmd` windows for the clients.

---

## Python Testers

Located in the `testers/` directory. Requires no extra dependencies beyond the Python standard library.

### socket_auto_plays.py — Automated Round Skip

Automates two players over Socket to fast-forward a game by a given number of rounds from the current one. Useful for testing mid/late-game states without playing manually.

**Usage:**

```bash
python3 testers/socket_auto_plays.py -g <game-id> -p1 <player1-name> -p2 <player2-name> -r <rounds>
```

| Flag | Required | Description                                        |
|------|----------|----------------------------------------------------|
| `-g` | yes      | Game ID to join                                    |
| `-p1`| yes      | Username of Player 1                               |
| `-p2`| yes      | Username of Player 2                               |
| `-r` | yes      | Number of rounds to advance from the current round |
| `-H` | no       | Server IP (default: `127.0.0.1`)                   |
| `-P` | no       | Server port (default: `8080`)                      |

**Example:**

```bash
python3 testers/socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5
```

This logs in as Alice and Bob, joins game `1`, and automatically plays 5 rounds from the current one.

**Remote server example:**

```bash
python3 testers/socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5 -H 192.168.1.10 -P 8080
```

> Make sure the server is running and the game with the given ID exists before launching the script.

---

## Full Command Reference

```
java -jar target/mesos.jar --server  <socket-port> <rmi-port> <recover-dir>
```
```
java -jar target/mesos.jar --client  --socket  --tui  <ip> <port>
```
```
java -jar target/mesos.jar --client  --socket  --gui  <ip> <port>
```
```
java -jar target/mesos.jar --client  --rmi     --tui  <ip> <port>
```
```
java -jar target/mesos.jar --client  --rmi     --gui  <ip> <port>
```
```
python3 testers/socket_auto_plays.py -g <id> -p1 <name> -p2 <name> -r <round> [-H <ip>] [-P <port>]
```
