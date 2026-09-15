# MESOS — Software Engineering Project 2026

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue.svg)
![Build](https://img.shields.io/badge/Build-passing-brightgreen.svg)

**Team:** Vitale · Wu · Romeo-Retez · Vaccai

> [!NOTE]
> 🇺🇸 The Mesos Board Game and all related artwork is the exclusive property of Cranio Creations.

> 🇮🇹 Il Gioco da tavolo Mesos e tutto il relativo materiale grafico è di esclusiva proprietà di Cranio Creations.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Implementation](#implementation)
3. [Build](#build)
4. [Running the Application](#running-the-application)
   - [Start the Server](#1-start-the-server)
   - [Start a Client](#2-start-a-client)
5. [Database & Leaderboard Setup](#database--leaderboard-setup)
6. [Resiliency & Game Persistence](#resiliency--game-persistence)
7. [Python Testers](#python-testers)
8. [Javadoc](#javadoc)
9. [Full Command Reference](#full-command-reference)

---

## Prerequisites

- **Java 25+** (the project compiles and targets Java 25 via Maven release properties)
- **Maven 3.8+** (to build)
- **Python 3.8+** (for testers)

---

## Implementation
| Requisiti Soddisfatti | Check |
|-----------------------|-------|
| Complete rules | 🟢 |
| Socket | 🟢 |
| RMI | 🟢 |
| TUI | 🟢 |
| GUI | 🟢 |
| Leaderboard DB | 🟢 |
| Multiple games | 🟢 |
| Resiliency to disconnections | 🟡 |
| Persistence | 🟢 |

Legend:
🟢 Completed | 🟡 Partial implementation | 🔴 Not implemented

> [!NOTE]
> The functionality `Resiliency to disconnections` has been implemented in a different way:
> if a player crash from a game it will enter the state `RecoverState` 
> (see also [Resiliency & Game Persistence](#resiliency--game-persistence)).

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

> [!IMPORTANT]
> **Running on a remote server (Non-local RMI setup)**:
> If the server is running on a remote machine and you want clients to connect via RMI from outside, Java RMI requires you to specify the server's public/reachable IP address via the `java.rmi.server.hostname` system property. Otherwise, clients will fail to connect:
> ```bash
> java -Djava.rmi.server.hostname=<server-ip> -jar server/target/mesos-server.jar 8080 1099 ./saved
> ```

---

### 2. Start a Client

Open a **new terminal** and run:

```bash
java -jar client/target/mesos-client.jar --client <connection> <interface> <server-ip> <port>
```

| Argument       | Options              | Description                            |
|----------------|----------------------|----------------------------------------|
| `<connection>` | `--socket` / `--rmi` | Network protocol to use                |
| `<interface>`  | `--tui` / `--gui`    | UI mode (TUI = terminal, GUI = JavaFX) |
| `<server-ip>`  | e.g. `127.0.0.1`     | IP address of the server               |
| `<port>`       | e.g. `8080` / `1099` | Port matching the chosen protocol (use the socket port for `--socket`, the RMI port for `--rmi`) |

**Command matrix** (local-machine examples, default ports `8080` socket / `1099` RMI):

| Connection | Interface | Command                                                                          |
|------------|-----------|----------------------------------------------------------------------------------|
| Socket     | TUI       | `java -jar client/target/mesos-client.jar --client --socket --tui 127.0.0.1 8080` |
| Socket     | GUI       | `java -jar client/target/mesos-client.jar --client --socket --gui 127.0.0.1 8080` |
| RMI        | TUI       | `java -jar client/target/mesos-client.jar --client --rmi --tui 127.0.0.1 1099`    |
| RMI        | GUI       | `java -jar client/target/mesos-client.jar --client --rmi --gui 127.0.0.1 1099`    |


The client jar also supports in-process test modes (no real server required):

```bash
java -jar client/target/mesos-client.jar --test-tui
java -jar client/target/mesos-client.jar --test-gui
```

---

## Python Testers

The testers live in `tools/testers/`. They need only the Python standard
library — no extra dependencies.

### socket_auto_plays.py — Automated Round Skip

Automates two players over Socket to fast-forward a game by a given number of rounds from the current one. Useful for testing mid/late-game states without playing manually.

**Usage** (from the repo root, writing the path directly):

macOS / Linux:

```bash
python3 tools/testers/socket_auto_plays.py -p1 <player1-name> -p2 <player2-name> -r <rounds>
```

Windows:

```
python tools\testers\socket_auto_plays.py -p1 <player1-name> -p2 <player2-name> -r <rounds>
```

| Flag | Required | Description                                        |
|------|----------|----------------------------------------------------|
| `-p1`| yes      | Username of Player 1                               |
| `-p2`| yes      | Username of Player 2                               |
| `-r` | yes      | Number of rounds to advance from the current round |
| `-H` | no       | Server IP (default: `127.0.0.1`)                   |
| `-P` | no       | Server port (default: `8080`)                      |

**Example** (from repo root):

```bash
python3 tools/testers/socket_auto_plays.py -p1 Alice -p2 Bob -r 5
```

On Windows:

```
python tools\testers\socket_auto_plays.py -p1 Alice -p2 Bob -r 5
```

This logs in as Alice and Bob and automatically plays 5 rounds from the current one.

**Remote server example:**

```bash
python3 tools/testers/socket_auto_plays.py -p1 Alice -p2 Bob -r 5 -H 192.168.1.10 -P 8080
```

> [!WARNING]
> Make sure the server is running and the players were playing a game (only them) and both of them
> need to execute an action `totem placement` (first p1 and then p2) before launching the script.

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
The server automatically persists all active game states as serialized `.ser` files in the specified recovery directory (e.g. `./saved`).

### 2. Server Crash Recovery
If the server crashes or restarts:
1. On startup, it recovers all unfinished games from the serialization folder.
2. The game enters a `RecoverState` (suspended).
3. Players can relaunch their clients and join again by logging with the **exact same nickname**.
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

### 1. Server App

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

**Remote Server Execution (Non-local RMI)**:
When running on a remote server, specify the reachable server IP using the `-Djava.rmi.server.hostname` property:
* **Without Database**:
  ```bash
  java -Djava.rmi.server.hostname=<server-ip> -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
  ```
* **With Database Support**:
  * **macOS / Linux**:
    ```bash
    DB_PASSWORD="your_password" java -Djava.rmi.server.hostname=<server-ip> -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
    ```
  * **Windows (Command Prompt)**:
    ```cmd
    set DB_PASSWORD=your_password
    java -Djava.rmi.server.hostname=<server-ip> -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
    ```
  * **Windows (PowerShell)**:
    ```powershell
    $env:DB_PASSWORD="your_password"
    java -Djava.rmi.server.hostname=<server-ip> -jar server/target/mesos-server.jar <socket-port> <rmi-port> <recover-dir>
    ```

### 2. Client App (Manual Startup)

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

### 3. Client App (In-Process Test Modes)

```bash
java -jar client/target/mesos-client.jar --test-tui
java -jar client/target/mesos-client.jar --test-gui
```

### 4. Python Testers

**macOS / Linux**:
```bash
python3 tools/testers/socket_auto_plays.py -p1 <name> -p2 <name> -r <rounds> [-H <ip>] [-P <port>]
```
**Windows**:
```cmd
python tools\testers\socket_auto_plays.py -p1 <name> -p2 <name> -r <rounds> [-H <ip>] [-P <port>]
```
