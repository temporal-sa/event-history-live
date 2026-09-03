# Temporal 디버그 빌드 — 준비 가이드

`git clone`부터 디버그 서버 실행까지 처음부터 끝까지 안내합니다. **macOS**, **Windows(WSL2)**,
**Linux**를 지원합니다. **Go 사전 지식이 없어도 됩니다.**

> 🌐 English: [`PREREQUISITES.md`](PREREQUISITES.md) ·
> 🖨️ 인쇄용: [`PREREQUISITES.html`](PREREQUISITES.html) (브라우저에서 열고 **인쇄 → PDF로 저장**;
> 모든 OS와 두 언어가 함께 인쇄됩니다).

---

## 0. 무엇을 빌드하나요

Temporal 소스 코드로부터 두 개의 명령줄 프로그램을 만듭니다 — `temporal-server-debug`(내부
타임아웃이 완화된 디버그 모드 Temporal 서버)와 `tdbg`(데이터베이스의 원시 데이터를 해독하는 도구)
— 그리고 이들이 필요로 하는 데이터베이스를 Docker로 실행합니다.

> **Go 코드를 작성하지 않습니다.** `make`가 빌드를 대신 실행하고 Go가 자동으로 컴파일합니다.
> Go를 *설치*만 하면 됩니다.

---

## 1. 시스템 요구사항

데이터베이스 스택(MySQL, Elasticsearch, Cassandra 등)은 Docker에서 실행되며 메모리를 많이
사용합니다. **여유 RAM 8GB 이상**(Docker Desktop에 6~8GB 이상 할당), **여유 디스크 약 15GB**,
인터넷 연결이 필요합니다(첫 빌드에서 수백 MB를 내려받습니다).

---

## 2. 도구 설치

네 가지 도구가 필요합니다: **Git**, **Go 1.26.4 이상**, **GNU Make**, **Docker**.
**Temporal CLI**도 있으면 편리합니다.

### macOS

[Homebrew](https://brew.sh)가 가장 쉽습니다. `xcode-select --install`이 Git과 Make를 제공합니다.

```bash
# 1) Xcode Command Line Tools (git + make 제공)
xcode-select --install

# 2) Homebrew (이미 설치했다면 건너뛰기)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 3) Go, 그리고 (선택) Temporal CLI + 최신 GNU Make
brew install go temporal make

# 4) Docker Desktop — 내려받아 설치한 뒤 한 번 실행하세요:
#    https://www.docker.com/products/docker-desktop/  (Apple Silicon·Intel 모두 지원)
```

> **Apple Silicon:** 이미지 호환성을 위해 Docker Desktop 설정(Settings → General)에서
> "Use Rosetta for x86/amd64 emulation"을 켜 두세요.

### Windows(WSL2) / Linux

**Windows는 먼저:** Temporal은 Linux 환경에서 빌드합니다. **WSL2 + Ubuntu**를 설치한 뒤
*나머지 모든 작업*을 Ubuntu 터미널에서 진행하세요. **PowerShell을 관리자 권한으로** 여세요:

```powershell
# Windows PowerShell (관리자) — WSL2 + Ubuntu 설치 후 재부팅
wsl --install
```

**Ubuntu / WSL2 / Linux:** Ubuntu 패키지의 Go는 대개 너무 오래되었으므로 공식 배포판을
설치하세요. (ARM 머신은 `amd64`를 `arm64`로 바꾸세요.)

```bash
# 1) Git + GNU Make
sudo apt update && sudo apt install -y git make curl

# 2) Go 1.26.4 (공식) — 기존 Go 삭제 후 /usr/local에 설치
curl -LO https://go.dev/dl/go1.26.4.linux-amd64.tar.gz
sudo rm -rf /usr/local/go && sudo tar -C /usr/local -xzf go1.26.4.linux-amd64.tar.gz

# 3) Go를 PATH에 추가 (이 줄을 ~/.profile에 추가한 뒤 셸을 다시 여세요)
export PATH=$PATH:/usr/local/go/bin

# 4) (선택) Temporal CLI
curl -sSf https://temporal.download/cli.sh | sh
```

**Docker.** *Windows(WSL2):*
[Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)를 설치하고
Settings → Resources → WSL Integration에서 Ubuntu를 켜세요. *네이티브 Linux:* Docker Engine을
설치하세요:

```bash
# 네이티브 Linux 전용 (Windows/WSL2는 Docker Desktop이 처리하므로 건너뛰기)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER      # 그런 다음 로그아웃 후 다시 로그인
```

### ✔ 확인

```bash
git --version
go version            # 설치된 Go; 필요 시 빌드가 1.26.4를 자동으로 받습니다
make --version
docker --version
docker compose version
```

> **Go 버전:** `go version`이 1.26.4보다 낮아도 보통 괜찮습니다 — 최신 Go가 첫 빌드 때 `go.mod`에
> 지정된 툴체인을 자동으로 내려받습니다.

---

## 3. 소스 코드 받기

Temporal 서버 저장소를 클론하고 `main`을 그대로 사용합니다 — **소스 수정은 필요 없습니다**.
이후 모든 명령은 이 폴더에서 실행합니다.
**WSL2에서는 `/mnt/c`가 아니라 Linux 홈(`~`) 안에 클론하세요** — 훨씬 빠릅니다.

```bash
git clone https://github.com/temporalio/temporal.git
cd temporal
```

---

## 4. 바이너리 빌드

**첫 빌드는 느립니다**(모든 Go 패키지를 내려받고 Go 1.26.4 툴체인을 가져올 수 있음) — 몇 분
걸리는 것이 정상입니다. 이후 빌드는 빠릅니다.

```bash
make temporal-server-debug     # -> ./temporal-server-debug (디버그 빌드: 내부 타임아웃 완화)
make tdbg                      # -> ./tdbg
make temporal-sql-tool         # -> ./temporal-sql-tool (DB 스키마 설치용)
```

> **디버그 빌드를 쓰는 이유?** `make temporal-server-debug`는 `TEMPORAL_DEBUG` 빌드 태그로
> 컴파일되어 서버 내부 타임아웃을 ×100으로 늘립니다 — 이 여유 덕분에 디버깅 세션(과 중단점)
> 동안 서버가 워커를 포기하지 않고 버틸 수 있습니다.

**✔ 확인**

```bash
ls -la temporal-server-debug tdbg temporal-sql-tool   # 세 파일이 있어야 합니다
./tdbg --help | head
```

---

## 5. 데이터베이스 실행 — **터미널 1**

Docker Compose로 MySQL, Temporal 웹 UI 등을 실행합니다. 계속 실행되므로 **이 터미널은 열어
두세요**. 먼저 Docker Desktop이 실행 중인지 확인하세요.

```bash
make start-dependencies         # MySQL :3306, Temporal UI :8080, Grafana, ...
# MySQL 로그에 "ready for connections"가 나올 때까지 기다리세요
```

---

## 6. 스키마 설치 — **터미널 2**

방금 실행한 MySQL 안에 Temporal 테이블을 생성합니다. 한 번만 실행하세요(초기화하려면 다시 실행).

```bash
cd temporal
make install-schema-mysql        # `temporal` + `temporal_visibility` DB 생성
```

---

## 7. 디버그 서버 실행 — **터미널 3**

빌드한 서버를 MySQL을 향하도록 실행합니다. 계속 실행되므로 열어 두세요.

```bash
./temporal-server-debug \
  --config-file config/development-mysql8.yaml \
  --allow-no-auth start
# gRPC :7233 · 웹 UI http://localhost:8080
```

---

## 8. 정상 동작 확인 — **터미널 4**

```bash
temporal operator namespace create --namespace default
# 대시보드 열기: http://localhost:8080
./tdbg --address 127.0.0.1:7233 cluster describe   # 클러스터 정보가 출력되어야 합니다
```

> ✅ **완료** — 네임스페이스가 생성되고 UI가 열리며 `tdbg`가 응답하면, 워크숍 환경이 준비된
> 것입니다. 🎉

---

## 9. 문제 해결

| 증상 | 해결 |
|------|------|
| `make: command not found` | Make가 설치되지 않았습니다 — 2단계 참고. |
| `Cannot connect to the Docker daemon` | Docker Desktop을 실행하세요(Linux: `sudo systemctl start docker`). |
| Go 버전으로 빌드 실패 | Go가 1.26.4 툴체인을 받도록 인터넷 연결 확인, `go version` 1.21 이상. |
| `port ... already in use` (7233/8080/3306) | 다른 Temporal/DB가 실행 중입니다 — 종료하세요. |
| WSL2 빌드가 매우 느림 | `/mnt/c/...`가 아니라 Linux 홈 `~` 아래에 클론하세요. |
| Docker 메모리 부족 | Docker Desktop 메모리를 8GB 이상으로 올리세요(Settings → Resources). |
| `temporal: command not found` | Temporal CLI는 선택 사항입니다 — 설치하거나 웹 UI를 쓰세요. |

### 정리

서버 터미널에서 `Ctrl+C`를 누른 뒤 Docker를 종료하세요:

```bash
make stop-dependencies
```

---

*참고: Temporal 저장소의 `CONTRIBUTING.md`와 `Makefile`. Go `1.26.4`, C 컴파일러
불필요(`CGO_ENABLED=0`).*
