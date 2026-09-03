# Temporal Debug Build — Setup Prerequisites

End-to-end from `git clone` to a running debug server. Covers **macOS**, **Windows (WSL2)**,
and **Linux**. **No prior Go experience needed.**

> 🌐 한국어: [`PREREQUISITES-ko-kr.md`](PREREQUISITES-ko-kr.md) ·
> 🖨️ Printable: [`PREREQUISITES.html`](PREREQUISITES.html) (open in a browser → **Print → Save as PDF**;
> it prints all operating systems and both languages).

---

## 0. What you'll build

Two command-line programs from Temporal's source code — `temporal-server-debug` (the Temporal
server in debug mode, with relaxed internal timeouts) and `tdbg` (a tool that decodes the raw
data in the database) — plus the databases they need, started with Docker.

> **You will NOT write any Go.** `make` runs the build for you and Go compiles automatically.
> You only need Go *installed*.

---

## 1. System requirements

The database stack (MySQL, Elasticsearch, Cassandra, and more) runs in Docker and is
memory-hungry. Have **≥ 8 GB RAM free** (allocate ≥ 6–8 GB to Docker Desktop), **~ 15 GB free
disk**, and internet (the first build downloads several hundred MB).

---

## 2. Install the tools

You need four tools: **Git**, **Go 1.26.4+**, **GNU Make**, **Docker**. The **Temporal CLI**
is a handy extra.

### macOS

Easiest via [Homebrew](https://brew.sh). `xcode-select --install` gives Git + Make.

```bash
# 1) Xcode Command Line Tools (gives you git + make)
xcode-select --install

# 2) Homebrew (skip if already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 3) Go, plus (optional) the Temporal CLI and a newer GNU Make
brew install go temporal make

# 4) Docker Desktop — download, install, and LAUNCH it once:
#    https://www.docker.com/products/docker-desktop/  (Apple Silicon & Intel both work)
```

> **Apple Silicon:** In Docker Desktop, keep "Use Rosetta for x86/amd64 emulation" enabled
> (Settings → General) for the widest image compatibility.

### Windows (WSL2) / Linux

**Windows first:** Temporal builds inside Linux. Install **WSL2 + Ubuntu**, then do
*everything else* inside the Ubuntu terminal. Open **PowerShell as Administrator**:

```powershell
# Windows PowerShell (Administrator) — installs WSL2 + Ubuntu, then reboot
wsl --install
```

**Ubuntu / WSL2 / Linux:** Ubuntu's packaged Go is usually too old — install the official one.
(ARM machines: replace `amd64` with `arm64`.)

```bash
# 1) Git + GNU Make
sudo apt update && sudo apt install -y git make curl

# 2) Go 1.26.4 (official) — remove old Go, install to /usr/local
curl -LO https://go.dev/dl/go1.26.4.linux-amd64.tar.gz
sudo rm -rf /usr/local/go && sudo tar -C /usr/local -xzf go1.26.4.linux-amd64.tar.gz

# 3) Put Go on your PATH (add this line to ~/.profile, then re-open the shell)
export PATH=$PATH:/usr/local/go/bin

# 4) (optional) Temporal CLI
curl -sSf https://temporal.download/cli.sh | sh
```

**Docker.** *Windows (WSL2):* install
[Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/) and enable
Settings → Resources → WSL Integration for Ubuntu. *Native Linux:* install Docker Engine:

```bash
# Native Linux only (skip on Windows/WSL2 — Docker Desktop handles it)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER      # then log out and back in
```

### ✔ Verify

```bash
git --version
go version            # your installed Go; the build auto-fetches 1.26.4 if needed
make --version
docker --version
docker compose version
```

> **Go version:** If `go version` is older than 1.26.4, that's usually fine — modern Go
> auto-downloads the toolchain pinned in `go.mod` on the first build.

---

## 3. Get the source code

Clone the Temporal server repo. All later commands run from this folder. **On WSL2, clone
inside the Linux home (`~`), not `/mnt/c`** — far faster.

```bash
git clone https://github.com/temporalio/temporal.git
cd temporal
```

---

## 4. Apply the workshop patch

> **Workshop-specific.** Raises the workflow-task timeout cap from 120 s to 15 min so you can
> hold a breakpoint inside workflow code for up to 15 minutes. Skip for a plain debug build.

```bash
# macOS (BSD sed):
sed -i '' 's/maxWorkflowTaskStartToCloseTimeout = 120 \* time.Second/maxWorkflowTaskStartToCloseTimeout = 15 * time.Minute/' \
  service/history/api/create_workflow_util.go

# Linux / WSL2 (GNU sed):
sed -i 's/maxWorkflowTaskStartToCloseTimeout = 120 \* time.Second/maxWorkflowTaskStartToCloseTimeout = 15 * time.Minute/' \
  service/history/api/create_workflow_util.go

# verify:
grep maxWorkflowTaskStartToCloseTimeout service/history/api/create_workflow_util.go
# expected -> maxWorkflowTaskStartToCloseTimeout = 15 * time.Minute
```

---

## 5. Build the binaries

The **first build is slow** (downloads all Go packages, may fetch the Go 1.26.4 toolchain) —
several minutes is normal. Later builds are fast.

```bash
make temporal-server-debug     # -> ./temporal-server-debug
make tdbg                      # -> ./tdbg
make temporal-sql-tool         # -> ./temporal-sql-tool (installs the DB schema)
```

**✔ Verify**

```bash
ls -la temporal-server-debug tdbg temporal-sql-tool   # three files should exist
./tdbg --help | head
```

---

## 6. Start the databases — **Terminal 1**

Starts MySQL, the Temporal Web UI, and more via Docker Compose. It keeps running — **leave this
terminal open**. Make sure Docker Desktop is running first.

```bash
make start-dependencies         # MySQL :3306, Temporal UI :8080, Grafana, ...
# wait until MySQL logs "ready for connections"
```

---

## 7. Install the schema — **Terminal 2**

Create the Temporal tables in MySQL. Run once (re-run any time for a clean DB).

```bash
cd temporal
make install-schema-mysql        # creates the `temporal` + `temporal_visibility` DBs
```

---

## 8. Run the debug server — **Terminal 3**

Start the server you built, pointed at MySQL. It keeps running — leave it open.

```bash
./temporal-server-debug \
  --config-file config/development-mysql8.yaml \
  --allow-no-auth start
# gRPC on :7233 · Web UI on http://localhost:8080
```

---

## 9. Verify everything works — **Terminal 4**

```bash
temporal operator namespace create --namespace default
# open the dashboard: http://localhost:8080
./tdbg --address 127.0.0.1:7233 cluster describe   # should print cluster info
```

> ✅ **Done** — If the namespace is created, the UI loads, and `tdbg` responds, your
> environment is ready for the workshop. 🎉

---

## 10. Troubleshooting

| Symptom | Fix |
|---------|-----|
| `make: command not found` | Make isn't installed — see step 2. |
| `Cannot connect to the Docker daemon` | Start Docker Desktop (Linux: `sudo systemctl start docker`). |
| Build fails on Go version | Ensure internet so Go can fetch the 1.26.4 toolchain; `go version` ≥ 1.21. |
| `port ... already in use` (7233/8080/3306) | Another Temporal/DB is running — stop it. |
| WSL2 build extremely slow | Clone under the Linux home `~`, never `/mnt/c/...`. |
| Docker runs out of memory | Raise Docker Desktop memory to ≥ 8 GB (Settings → Resources). |
| `temporal: command not found` | Temporal CLI is optional — install it or use the Web UI. |

### Teardown

Press `Ctrl+C` in the server terminal, then stop Docker:

```bash
make stop-dependencies
```

---

*Reference: the Temporal repo's `CONTRIBUTING.md` and `Makefile`. Go `1.26.4`, no C compiler
required (`CGO_ENABLED=0`).*
