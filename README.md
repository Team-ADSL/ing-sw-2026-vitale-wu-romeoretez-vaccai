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

### Client Options Reference

```
java -jar target/mesos.jar --client <connection> <interface> <server-ip> <port>
```

| Argument       | Options              | Description                         |
|----------------|----------------------|-------------------------------------|
| `<connection>` | `--socket` / `--rmi` | Network protocol to use             |
| `<interface>`  | `--tui` / `--gui`    | UI mode (GUI not yet implemented)   |
| `<server-ip>`  | e.g. `127.0.0.1`     | IP address of the server            |
| `<port>`       | e.g. `8080` / `1099` | Port matching the chosen protocol   |

---

## Typical Local Session

```
Terminal 1 (server):
  java -jar target/mesos.jar --server 8080 1099 ./saved

Terminal 2 (socket client):
  java -jar target/mesos.jar --client --socket --tui 127.0.0.1 8080

Terminal 3 (rmi client):
  java -jar target/mesos.jar --client --rmi --tui 127.0.0.1 1099
```

---

## Python Testers

Located in the `testers/` directory. Requires no extra dependencies beyond the Python standard library.

### socket_auto_plays.py — Automated Round Skip

Automates two players over Socket to fast-forward a game to a target round. Useful for testing mid/late-game states without playing manually.

**Usage:**

```bash
python3 testers/socket_auto_plays.py -g <game-id> -p1 <player1-name> -p2 <player2-name> -r <target-round>
```

| Flag | Required | Description                                      |
|------|----------|--------------------------------------------------|
| `-g` | yes      | Game ID to join                                  |
| `-p1`| yes      | Username of Player 1                             |
| `-p2`| yes      | Username of Player 2                             |
| `-r` | yes      | Target round to reach (script stops when reached)|
| `-H` | no       | Server IP (default: `127.0.0.1`)                 |
| `-P` | no       | Server port (default: `8080`)                    |

**Example:**

```bash
python3 testers/socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5
```

This logs in as Alice and Bob, joins game `1`, and plays rounds automatically until round `5` is reached.

**Remote server example:**

```bash
python3 testers/socket_auto_plays.py -g 1 -p1 Alice -p2 Bob -r 5 -H 192.168.1.10 -P 8080
```

> Make sure the server is running and the game with the given ID exists before launching the script.

---

## Full Command Reference

```
java -jar target/mesos.jar --server  <socket-port> <rmi-port> <recover-dir>
java -jar target/mesos.jar --client  --socket  --tui  <ip> <port>
java -jar target/mesos.jar --client  --socket  --gui  <ip> <port>
java -jar target/mesos.jar --client  --rmi     --tui  <ip> <port>
java -jar target/mesos.jar --client  --rmi     --gui  <ip> <port>
python3 testers/socket_auto_plays.py -g <id> -p1 <name> -p2 <name> -r <round> [-H <ip>] [-P <port>]
```
