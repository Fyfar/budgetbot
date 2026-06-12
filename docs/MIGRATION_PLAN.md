# BudgetBot — "Make It Great Again" Migration Plan

## Context

BudgetBot is a single-user app (Telegram bot + Monobank integration) running on an **Oracle
Always-Free AMD `E2.1.Micro` (1 GB RAM, 1/8 burstable OCPU)**. It is deployed via Docker Swarm and
suffers from instability: timeouts and intermittent errors.

Root causes found during review:
- Balance detection was a **polling scheduler** (`BalanceScheduler`) hammering Monobank's
  `/personal/client-info` (rate-limited to **1 req/60s per token**) with **no HTTP timeouts**, so a
  hung call stalls the thread → the "timeouts and errors".
- A **half-finished webhook migration** exists (commit `f22905c`): `WebhookController`
  (`POST /personal/balance/webhook`) + DTOs were added and the old scheduler deleted, **but** there
  is no code to (a) register the webhook with Monobank, (b) answer Monobank's GET validation
  handshake, or (c) authenticate the endpoint. So push delivery never works end-to-end.
- A default (uncapped) JVM on a **1 GB box with no swap** invites OOM-kills.
- Software is **end-of-life**: Spring Boot 2.6.6 + Java 11.

**Intended outcome:** a stable, predictable, easy-to-deploy/fix app that receives balance changes
via a **Monobank webhook** exposed through a **Cloudflare Tunnel** (no inbound ports opened on
Oracle), rebuilt on a **lean, low-memory Micronaut + Java 21 stack**, deployed with a single
`docker compose up -d`.

## Decisions (confirmed with user)

- **Framework:** **rewrite Spring Boot → Micronaut 4.x on Java 21**. Chosen for memory/startup:
  Micronaut does DI/AOP at compile time (no runtime reflection, no proxies, no classpath scanning),
  giving ~80–150 MB JVM RSS vs Spring's ~200–350 MB on this box, and an easy path to a ~84 MB
  native image later. Micronaut's APIs (`@Scheduled`, `@ConfigurationProperties`,
  `ApplicationEventPublisher`, Micronaut Data repos) mirror Spring, making the port mechanical.
- **Runtime/deploy:** **single `docker-compose.yml` (no Swarm)**, `restart: always`, memory-capped.
- **Detection:** **webhook (push)** — implemented natively in Micronaut (not ported from the
  half-built Spring controller).
- **Inbound exposure:** **Cloudflare Tunnel** — both Oracle firewalls stay closed; the app port is
  never published to the host (cloudflared reaches it over the internal compose network).
- **Native image:** **optional follow-on (Phase 6)** — low-risk on Micronaut, deferred until the
  JVM build is stable.

## Prerequisites (manual, before/at deploy)

- A domain managed in **Cloudflare** (free plan fine).
- **Cloudflare Zero Trust** → create a **named tunnel**, copy its **tunnel token**.
- Monobank API token(s) — already in `data/monobank.json` (`tokenList`).

---

## Step 0 — Save this plan into the repo

Copy this file to **`docs/MIGRATION_PLAN.md`** and commit, so it can be versioned and edited.
(Plan mode could not write into the repo directly.)

## Phase 0 — Server hygiene (host, one-time, no app change) ✅ DONE

Biggest stability win, independent of all code work.

1. Create a **2 GB swapfile** (`fallocate` → `mkswap` → `swapon`), persist in `/etc/fstab`.
   Standard OOM-kill fix on the 1 GB Micro (even with Micronaut's lower footprint, swap is cheap
   insurance).
2. `vm.swappiness=10` via `/etc/sysctl.d/`.
3. Tear down the old Swarm deploy: `docker stack rm <stack>`, `docker swarm leave --force`, remove
   old `docker config` objects and the named volume (after data is moved to a bind mount in Phase 4).

## Phase 1 — Rewrite to Micronaut 4 + Java 21 ✅ DONE (commit ae4dbf3)

The codebase is small (~50 mostly-simple files). This is a **mechanical port**, not a redesign:
keep the same package structure, domain models, DTOs, MapStruct mappers, Lombok, and the
`telegrambots` library (framework-agnostic). Reimplement only the framework-coupled glue per the
mapping below. **The bulk of the effort/risk is re-testing**, so port the test suite alongside.

**Build (`pom.xml`):**
- Replace `spring-boot-starter-parent` with the **Micronaut Platform BOM** + `micronaut-parent`,
  `<java.version>21</java.version>`, and the `micronaut-maven-plugin`.
- Annotation processors: `micronaut-inject-java`, `micronaut-data-processor`, `mapstruct-processor`,
  `lombok` + `lombok-mapstruct-binding`. Change MapStruct component model from
  `-Amapstruct.defaultComponentModel=spring` → **`jsr330`**.
- Drop Spring starters; add Micronaut features: `micronaut-http-server-netty`,
  `micronaut-http-client`, `micronaut-data-hibernate-jpa`, `micronaut-jdbc-hikari`,
  `micronaut-management` (health), `h2`, `micronaut-test-junit5`.

**Spring → Micronaut mapping:**

| Concern | Spring (now) | Micronaut (target) | Files |
|---|---|---|---|
| Bootstrap | `SpringApplication.run` | `Micronaut.run` | `Application.java` |
| Beans/DI | `@Service`/`@Component`/`@Autowired` | `@Singleton` + ctor inject (keep Lombok `@RequiredArgsConstructor`) | all services/listeners |
| Config | `@ConfigurationProperties` | `@ConfigurationProperties` (near-identical) | `MonobankProperties`, `TelegramBotProperties` |
| Web | `@RestController`/`@RequestMapping` | `@Controller` + `@Get`/`@Post` | `bank/webhook/WebhookController.java` |
| Repos | Spring Data JPA | **Micronaut Data JPA** `@Repository` (derived queries + `@Query` for the custom ones) | `BalanceHistoryRepository`, `UserRepository`, `ConfigRepository`, `DateTimeRepository*` |
| Events | `ApplicationEventPublisher` + `@EventListener` | same names in `io.micronaut.context.event` / `io.micronaut.runtime.event.annotation` | `BalanceServiceImpl`, `BalanceChangeListener`, bot update events |
| Scheduling | `@Scheduled(cron=…)` | `io.micronaut.scheduling.annotation.@Scheduled(cron=…)` | `DailyReportNotifier` + new drift-check |
| HTTP client | OpenFeign | Micronaut declarative `@Client` (built-in timeout config) | `bank/client/MonobankClient.java`, `BankConfiguration` |
| Startup hook | `ApplicationReadyEvent` | `StartupEvent` | new webhook registrar |
| Logging | Lombok `@Log4j2` | Lombok `@Slf4j` (Micronaut → logback/slf4j) | all logged classes |
| Health | actuator `/actuator/health` | `micronaut-management` `/health` | n/a |

**Config (`application.yml`):** rename keys — `server.port` → `micronaut.server.port: 7070`;
datasource under `datasources.default` (H2 file URL unchanged: `jdbc:h2:file:./data/database`);
JPA/Hibernate under `jpa.default.properties`. Keep the existing `./data/*.json` config-loading
approach (`PropertyProvider` reads `file:./data/monobank.json` / `telegram.json` — keep as-is).

**Entities:** already need `jakarta.persistence` (Micronaut Data uses it). Convert imports if any
are still `javax.persistence`.

**Tests:** `@SpringBootTest`/`@DataJpaTest` → `@MicronautTest`; keep JUnit 5 + WireMock (bump to
`org.wiremock:wiremock:3.x`). Port `BudgetServiceTest`, `BalanceSchedulerTest` (rename/repurpose to
webhook test), and the integration tests. Goal: `./mvnw clean test` green.

## Phase 2 — Resilience hardening (correctness + robustness) ✅ DONE (commit 8b57f2e)

- **HTTP client timeouts** on the Micronaut `@Client` (`read-timeout`, `connect-timeout` ~5–10 s) —
  the direct fix for the original "hung call" timeouts.
- **Integer-compare bug**: `BalanceServiceImpl` compares boxed `Integer` with `==`
  (`lastBalance.get() == newBalance`) — wrong for values > 127. Use `.equals`/`intValue()`.
- **Health**: expose `/health` (internal only) for the Docker healthcheck.
- **Dead-config cleanup**: drop unused polling knobs (`monobank.scheduler-delay`,
  `MonobankProperties.schedulerDelay`) and unused `ClientInfoFailEvent`.

## Phase 3 — Complete the webhook (push), in Micronaut ✅ DONE (commit 54cb062)

1. **Registration client** — add to the Micronaut `MonobankClient`: `@Post("/personal/webhook")`
   with header `X-Token` and a `SetWebhookRequest { webHookUrl }` body.
2. **Startup registrar** — `@EventListener void onStartup(StartupEvent e)` that loops
   `MonobankSecretProperties.tokenList` and registers the public URL (from config
   `monobank.webhook-public-url` + `monobank.webhook-secret`). Space calls if >1 token (set-webhook
   is ~1/60s/token).
3. **GET validation handshake** — the `@Controller` must answer Monobank's `GET` to the webhook URL
   with **HTTP 200** (it validates the URL via GET before delivering). Add a `@Get` handler
   alongside the `@Post`.
4. **Endpoint auth** — path becomes `/personal/balance/webhook/{secret}`; mismatched secret → 404.
   *(Optional later: verify Monobank `X-Sign` ECDSA header.)*
5. **Self-healing drift check** — Monobank **disables the webhook after 3 failed deliveries** (5 s
   timeout; retries at 60 s/600 s). Add a `@Scheduled` daily job that calls `getClientInfo` and
   re-registers if `ClientInfoDto.webHookUrl` ≠ expected. Key to "stable & predictable".
6. **No host port** — app listens on `0.0.0.0:7070` *inside the container only*; cloudflared reaches
   it via the private compose network (`http://bot:7070`). Nothing published to the host = zero
   inbound exposure.

## Phase 4 — Dockerize properly (single compose, no swarm) ✅ DONE (commit 2d62709)

- **`Dockerfile`** → multi-stage: `maven:3.9-eclipse-temurin-21` (build) → `eclipse-temurin:21-jre`
  (runtime). Removes the hardcoded jar name + EOL `openjdk:11`; `docker compose up --build` is fully
  reproducible without Maven on the box.
- **`docker-compose.yml`** (Compose v2, no `deploy:`/swarm), two services:
  - `bot`: `build: .`, `restart: always`, `mem_limit: 384m` (Micronaut headroom),
    `environment: JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65.0`, bind mount **`./data:/data`**
    (H2 DB + `monobank.json`/`telegram.json`), json-file logging `max-size`/`max-file`, healthcheck
    → `/health`. **No `ports:`**.
  - `cloudflared`: `image: cloudflare/cloudflared:latest`, `restart: always`,
    `command: tunnel run`, `environment: TUNNEL_TOKEN=${TUNNEL_TOKEN}`; reaches `http://bot:7070`.
- **Secrets** replace Swarm configs: `monobank.json` + `telegram.json` in host `./data` (already
  `.gitignore`d); `TUNNEL_TOKEN` + `monobank.webhook-secret` in a gitignored **`.env`**.
- Update **`README.md`** Deploy section: swarm → `docker compose up -d --build`.

## Phase 5 — Cloudflare Tunnel + Monobank registration 🔜 NEXT (see REMAINING_PHASES.md for full step-by-step)

1. Cloudflare Zero Trust → create named tunnel; add public hostname `budgetbot.<domain>` → service
   `http://bot:7070`; copy token into `.env`. DNS CNAME auto-created. (Token-only remote tunnel; no
   `config.yml`.)
2. `docker compose up -d` → `docker compose logs cloudflared` shows **connected**; dashboard
   hostname **healthy**.
3. Phase-3 registrar registers `https://budgetbot.<domain>/personal/balance/webhook/<secret>` with
   Monobank (or once manually: `curl -X POST https://api.monobank.ua/personal/webhook
   -H "X-Token: <token>" -d '{"webHookUrl":"https://budgetbot.<domain>/personal/balance/webhook/<secret>"}'`).

## Phase 6 — (Optional) GraalVM native image

Now low-risk on Micronaut (compile-time DI, minimal reflection). Build with
`./mvnw package -Dpackaging=native-image` (GraalVM/Mandrel), swap the Docker runtime stage for a
distroless base. Target ~84 MB RSS / ~50 ms start. Do this only after the JVM build is stable in
production; keep the JVM image as fallback.

## Verification

**Local (before deploy):**
- `./mvnw clean test` → green (Micronaut port intact).
- `docker compose up --build`; `docker compose exec bot curl -fsS http://localhost:7070/health` → `UP`.
- `GET` the webhook path → **200** (handshake). Wrong secret → 404.
- `POST` a sample `StatementItem` JSON to the secret path → expect a Telegram balance-change message
  (exercises `BalanceServiceImpl` → `BalanceChangeListener`).
- `docker stats` → bot RSS notably below the old Spring footprint (~120–180 MB expected).

**On the server (after deploy):**
- `docker compose logs -f cloudflared` → tunnel connected; Cloudflare hostname healthy.
- `curl https://budgetbot.<domain>/personal/balance/webhook/<secret>` (GET) → 200.
- Real **1 UAH** transaction → Monobank push → Telegram message within seconds.
- `free -h` + `docker stats` → comfortable RAM headroom (Micronaut + cap + swap).
- Restart `bot`; confirm the daily drift-check re-registers and pushes still arrive.

## Rollback / safety

- Old AMD/Swarm setup stays untouched until the new compose stack is verified — migrate by addition.
- `./data` is bind-mounted → H2 DB + config survive rebuilds and are trivially backed up (copy dir).
- Last-resort fallback if the webhook misbehaves: a timeout-guarded, ≤1/60s polling job (old
  `BalanceScheduler` logic, reimplemented in Micronaut) — not part of the plan.

## Out of scope (noted for later)

- ARM (Ampere A1) migration — decided separately; plan targets the current AMD box.
- `X-Sign` webhook signature verification (optional hardening).
- Replacing H2 with a server DB — unnecessary for a single user.
