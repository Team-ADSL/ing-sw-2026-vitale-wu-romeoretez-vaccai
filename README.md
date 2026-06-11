# MESOS — Software Engineering Project 2026

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

**Team:** Vitale · Wu · Romeo-Retez · Vaccai

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Build](#build)
3. [Running the Application](#running-the-application)
   - [Start the Server](#1-start-the-server)
   - [Start a Socket Client](#2-start-a-socket-client)
   - [Start an RMI Client](#3-start-an-rmi-client)
   - [Start a GUI Client (JavaFX)](#4-start-a-gui-client-javafx)
   - [Client Options Reference](#client-options-reference)
4. [Typical Local Session](#typical-local-session)
5. [Quick Start Scripts](#quick-start-scripts-toolsstart)
6. [Database & Leaderboard Setup](#database--leaderboard-setup)
7. [Resiliency & Game Persistence](#resiliency--game-persistence)
8. [Python Testers](#python-testers)
9. [Javadoc](#javadoc)
10. [Full Command Reference](#full-command-reference)

---

## Prerequisites

- **Java 25+** (the project compiles and targets Java 25 via Maven release properties)
- **Maven 3.8+** (to build)
- **Python 3.8+** (for testers)

---

## Build

The project is a multi-module Maven build (`shared`, `server`, `client`). From the repo root:

```bash
mvn package
```

Add `-DskipTests` to skip unit tests and speed up the build:

```bash
mvn package -DskipTests
```

Two shaded jars are produced:

| Jar | Path | Main class |
|-----|------|------------|
| Server | `server/target/mesos-server.jar` | `org.adsl.server.ServerApp` |
| Client | `client/target/mesos-client.jar` | `org.adsl.client.ClientApp` |

The server jar contains only `server/` + `shared/` classes; the client jar contains only `client/` + `shared/` classes — the two layers cannot import each other.

---

## Running the Application

### 1. Start the Server

Open a terminal and run:

```bash
java -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-directory>
```

| Argument             | Description                                              |
|----------------------|----------------------------------------------------------|
| `<socket-port>`      | Port for Socket connections (e.g. `8080`)               |
| `<rmi-port>`         | Port for RMI registry (e.g. `1099`)                     |
| `<recover-directory>`| Directory for game persistence files (e.g. `./saved`)  |

**Example:**

```bash
java -jar server/target/mesos-server.jar 8080 1099 ./saved
```

> [!NOTE]
> The two ports must be different and in the range 1024–65535.
> The server starts both a Socket listener and an RMI registry.

---

### 2. Start a Socket Client

Open a **new terminal** and run:

```bash
java -jar client/target/mesos-client.jar --client --socket --tui <server-ip> <socket-port>
```

**Example (local machine):**

```bash
java -jar client/target/mesos-client.jar --client --socket --tui 127.0.0.1 8080
```

---

### 3. Start an RMI Client

Open a **new terminal** and run:

```bash
java -jar client/target/mesos-client.jar --client --rmi --tui <server-ip> <rmi-port>
```

**Example (local machine):**

```bash
java -jar client/target/mesos-client.jar --client --rmi --tui 127.0.0.1 1099
```

---

### 4. Start a GUI Client (JavaFX)

The GUI client uses JavaFX 21 and is bundled inside the client jar
(`win`, `linux`, `mac`, `mac-aarch64` natives are all included).

Over Socket:

```bash
java -jar client/target/mesos-client.jar --client --socket --gui <server-ip> <socket-port>
```

Over RMI:

```bash
java -jar client/target/mesos-client.jar --client --rmi --gui <server-ip> <rmi-port>
```

**Examples (local machine):**

```bash
java -jar client/target/mesos-client.jar --client --socket --gui 127.0.0.1 8080
```
```bash
java -jar client/target/mesos-client.jar --client --rmi    --gui 127.0.0.1 1099
```

> [!TIP]
> Java 25 may print warnings about native access and `sun.misc.Unsafe`; they
> come from JavaFX itself and can be silenced by appending
> `--enable-native-access=ALL-UNNAMED` (cosmetic only — the jar runs fine
> without it).

---

### Client Options Reference

```
java -jar client/target/mesos-client.jar --client <connection> <interface> <server-ip> <port>
```

| Argument       | Options              | Description                         |
|----------------|----------------------|-------------------------------------|
| `<connection>` | `--socket` / `--rmi` | Network protocol to use             |
| `<interface>`  | `--tui` / `--gui`    | UI mode (TUI = terminal, GUI = JavaFX) |
| `<server-ip>`  | e.g. `127.0.0.1`     | IP address of the server            |
| `<port>`       | e.g. `8080` / `1099` | Port matching the chosen protocol   |

The client jar also supports in-process test modes (no real server required):

```bash
java -jar client/target/mesos-client.jar --test-tui
java -jar client/target/mesos-client.jar --test-gui
java -jar client/target/mesos-client.jar --test-server-connection
```

---

## Typical Local Session

Terminal 1 (server):
```
  java -jar server/target/mesos-server.jar 8080 1099 ./saved
```
Terminal 2 (socket client, TUI):

```
  java -jar client/target/mesos-client.jar --client --socket --tui 127.0.0.1 8080
```
Terminal 3 (rmi client, TUI):

```
  java -jar client/target/mesos-client.jar --client --rmi --tui 127.0.0.1 1099
```
Terminal 4 (socket client, GUI):

```
  java -jar client/target/mesos-client.jar --client --socket --gui 127.0.0.1 8080
```

---

## Quick Start Script (`tools/start/run`)

The `tools/start/` folder contains a centralized, cross-platform launcher script that automates starting a full local session — server + any number of clients — using a single command.

The script runs on **Python 3** (a project prerequisite). For convenience, shell wrappers are provided:
* **Windows**: `tools\start\run.bat`
* **macOS/Linux**: `./tools/start/run.sh`

### Usage & Arguments

```bash
# Windows
tools\start\run.bat [options]
```
```bash
# macOS/Linux
./tools/start/run.sh [options]
```

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--tui [count]` | `int` (optional) | `0` | Number of TUI clients to launch. Specifying `--tui` alone launches `1`. |
| `--gui [count]` | `int` (optional) | `0` | Number of GUI clients to launch. Specifying `--gui` alone launches `1`. |
| `--clean` | - | - | Kills active server instances and deletes the `./saved` folder before starting. |
| `--nobuild` | - | - | Skips the Maven compilation step (`mvn package`) to launch faster. |

> [!IMPORTANT]
> The total number of players (`tui` + `gui`) must be between **2 and 5**. 
> If no clients are specified, the launcher defaults to **1 TUI and 1 GUI client** (2 players).

### Examples

* **Launch 2 TUI clients (compiling first)**:
  ```bash
  tools\start\run.bat --tui 2
  ```

* **Launch 3 GUI clients, skipping compilation and cleaning old saves**:
  ```bash
  tools\start\run.bat --gui 3 --clean --nobuild
  ```

* **Launch a custom mixed session with 2 TUI and 3 GUI clients (total 5 players)**:
  ```bash
  tools\start\run.bat --tui 2 --gui 3
  ```

* **Launch a default 2-player session (equivalent to `--tui 1 --gui 1`)**:
  ```bash
  tools\start\run.bat
  ```

> [!NOTE]
> - By default, clients alternate connection protocols: player 1 uses Socket, player 2 uses RMI, player 3 Socket, and so on.
> - The launcher automatically waits for the server to be fully ready on port 8080 before opening the client windows.
> - If you receive a `Permission denied` error on macOS/Linux for `run.sh`, run `chmod +x tools/start/run.sh` first.

---

## Python Testers

The testers live in `tools/testers/`. They need only the Python standard
library — no extra dependencies.

> [!IMPORTANT]
> **Where to run from — no `cd` needed.**
> Stay in the **repository root** (the folder that contains `pom.xml` and the
> `tools/` directory) and type the full path to the script directly:
> `tools/testers/socket_auto_plays.py`.
> You do **not** have to `cd` into `tools/testers` first. Python automatically
> adds the script's own folder to its import path, so the sibling helper module
> `socket_client_utils.py` is found no matter which directory you launched from.
>
> Use `python` on Windows and `python3` on macOS / Linux.

### socket_auto_plays.py — Automated Round Skip

Automates two players over Socket to fast-forward a game by a given number of rounds from the current one. Useful for testing mid/late-game states without playing manually.

**Usage** (from the repo root, writing the path directly):

macOS / Linux:

```bash
python3 tools/testers/socket_auto_plays.py -g <game-id> -p1 <player1-name> -p2 <player2-name> -r <rounds>
```

Windows:

```
python tools\testers\socket_auto_plays.py -g <game-id> -p1 <player1-name> -p2 <player2-name> -r <rounds>
```

| Flag | Required | Description                                        |
|------|----------|----------------------------------------------------|
| `-g` | yes      | Game ID to join                                    |
| `-p1`| yes      | Username of Player 1                               |
| `-p2`| yes      | Username of Player 2                               |
| `-r` | yes      | Number of rounds to advance from the current round |
| `-H` | no       | Server IP (default: `127.0.0.1`)                   |
| `-P` | no       | Server port (default: `8080`)                      |

**Example** (from repo root):

```bash
python3 tools/testers/socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5
```

On Windows:

```
python tools\testers\socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5
```

This logs in as Alice and Bob, joins game `1`, and automatically plays 5 rounds from the current one.

**Remote server example:**

```bash
python3 tools/testers/socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5 -H 192.168.1.10 -P 8080
```

> [!WARNING]
> Make sure the server is running and the game with the given ID exists before launching the script.

---

## Database & Leaderboard Setup

The project includes an **all-time global leaderboard** functionality that persists players' match results. This runs on a **MySQL** server.

### 1. Requirements & Fallback
If the server cannot connect to the database (e.g. because the MySQL service is down or credentials are missing), it will **gracefully fall back** to an in-memory repository (`NoGameDAO`). 
- In this fallback mode, matches will run fine, but they won't be saved in the database, and the overall leaderboard will be unavailable.

### 2. Configuration
The database connection settings are hardcoded in [DatabaseConfig.java](file:///C:/Users/Lorenzo/IdeaProjects/ing-sw-2026-vitale-wu-romeoretez-vaccai/server/src/main/java/org/adsl/server/db/DatabaseConfig.java):
* **Host:** `localhost`
* **Port:** `3306`
* **Database Name:** `game_leaderboard` (automatically created by the server)
* **User:** `root`
* **Password:** Read from the `DB_PASSWORD` environment variable.

### 3. Running with Database Support
1. Make sure a MySQL server is running locally on port `3306`.
2. Define the `DB_PASSWORD` environment variable with your MySQL root password:
   * **macOS/Linux:**
     ```bash
     export DB_PASSWORD="your_password"
     java -jar server/target/mesos-server.jar 8080 1099 ./saved
     ```
   * **Windows (Command Prompt):**
     ```cmd
     set DB_PASSWORD=your_password
     java -jar server/target/mesos-server.jar 8080 1099 ./saved
     ```
   * **Windows (PowerShell):**
     ```powershell
     $env:DB_PASSWORD="your_password"
     java -jar server/target/mesos-server.jar 8080 1099 ./saved
     ```
   * **IntelliJ IDEA:** Open the Server Run Configuration and add `DB_PASSWORD=your_password` in the *Environment variables* field.

---

## Resiliency & Game Persistence

The server features robust connection recovery and crash-resiliency mechanisms.

### 1. Game Auto-Saving
The server automatically persists all active game states as serialized `.ser` files in the specified recovery directory (e.g. `./saved` or `./saves`).

### 2. Server Crash Recovery
If the server crashes or restarts:
1. On startup, it recovers all unfinished games from the serialization folder.
2. The game enters a `RecoverState` (suspended).
3. Players can relaunch their clients and attempt to join again using the **exact same nickname**.
4. Once all active players of that game have reconnected, the server automatically replays the game transcript, restores each client's state, and resumes the match.

### 3. Disconnection & Timeout Detection
* The server runs a background timeout checker that pings connected clients.
* If a client fails to ping within **20 seconds** (default threshold, checked every **5 seconds**), the server flags the player as disconnected.
* Disconnected players can reconnect to their ongoing match at any time using their nickname.

---

## Javadoc

### Generate

From the repo root, run:

```bash
mvn javadoc:aggregate
```

This builds a single aggregated Javadoc site for all three modules (`shared`, `server`, `client`) and writes it to:

```
target/reports/apidocs/index.html
```

To generate per-module docs instead:

```bash
mvn javadoc:javadoc
```

Each module's output lands in its own `target/reports/apidocs/` folder:

```
shared/target/reports/apidocs/index.html
server/target/reports/apidocs/index.html
client/target/reports/apidocs/index.html
```

Add `-DskipTests` to skip tests if you only want docs:

```bash
mvn javadoc:aggregate -DskipTests
```

---

### Open in a Browser

After generating, open the HTML file directly:

**macOS / Linux:**
```bash
open target/reports/apidocs/index.html
```

**Windows (Command Prompt):**
```
start target\reports\apidocs\index.html
```

**Windows (PowerShell):**
```powershell
Invoke-Item target\reports\apidocs\index.html
```

---

### Open in IntelliJ IDEA

#### Option A — Project panel (folder navigation)

1. In the **Project** panel (left sidebar), expand the module you want:
   - Aggregated: `ing-sw-2026-… → target → reports → apidocs`
   - Per-module: `server → target → reports → apidocs`
2. Double-click **`index.html`**.
3. In the floating browser toolbar that appears at the top-right of the editor, click the browser icon (Chrome, Firefox, etc.) to open in an external browser, or click the **Built-in preview** icon (magnifying glass) to read it inside IntelliJ.

> **Tip:** If the `target` folder is not visible, enable **Show Excluded Files** via the gear icon (⚙) at the top of the Project panel.

#### Option B — IntelliJ built-in Javadoc tool

1. Open any class or interface in the editor.
2. Place the cursor on a class / method name.
3. Press **Ctrl+Q** (Windows/Linux) or **F1** (macOS) to show the quick documentation popup.
4. Click **View in external documentation** (the globe icon) in the popup to jump to the full HTML page.

#### Option C — External Documentation URL

If you have already generated the docs and want to set a persistent link:

1. Go to **File → Settings → Tools → External Documentation**.
2. Add a new entry for `org.adsl` pointing to the absolute path of `target/reports/apidocs/`.

---

## Full Command Reference

### 1. Launcher Script (Recommended)

**Windows**:
```cmd
tools\start\run.bat [--tui <count>] [--gui <count>] [--clean] [--nobuild]
```
**macOS / Linux**:
```bash
./tools/start/run.sh [--tui <count>] [--gui <count>] [--clean] [--nobuild]
```

### 2. Server App

**Without Database (Fallback Mode)**:
```bash
java -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
```

**With Database Support**:
* **macOS / Linux**:
  ```bash
  DB_PASSWORD="your_password" java -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
  ```
* **Windows (Command Prompt)**:
  ```cmd
  set DB_PASSWORD=your_password
  java -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
  ```
* **Windows (PowerShell)**:
  ```powershell
  $env:DB_PASSWORD="your_password"
  java -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
  ```

### 3. Client App (Manual Startup)

**TUI over Socket**:
```bash
java -jar client/target/mesos-client.jar --client --socket --tui <server-ip> <socket-port>
```
**GUI over Socket**:
```bash
java -jar client/target/mesos-client.jar --client --socket --gui <server-ip> <socket-port>
```
**TUI over RMI**:
```bash
java -jar client/target/mesos-client.jar --client --rmi --tui <server-ip> <rmi-port>
```
**GUI over RMI**:
```bash
java -jar client/target/mesos-client.jar --client --rmi --gui <server-ip> <rmi-port>
```

### 4. Client App (In-Process Test Modes)

```bash
java -jar client/target/mesos-client.jar --test-tui
java -jar client/target/mesos-client.jar --test-gui
java -jar client/target/mesos-client.jar --test-server-connection
```

### 5. Python Testers

**macOS / Linux**:
```bash
python3 tools/testers/socket_auto_plays.py -g <id> -p1 <name> -p2 <name> -r <rounds> [-H <ip>] [-P <port>]
```
**Windows**:
```cmd
python tools\testers\socket_auto_plays.py -g <id> -p1 <name> -p2 <name> -r <rounds> [-H <ip>] [-P <port>]
```
