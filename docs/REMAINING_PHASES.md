# BudgetBot — Remaining Phases (Detailed Execution Guide)

> This is a **hand-off / execution** document. Phases 1–4 are **DONE and committed**. Phase 5 is the
> next step (server deploy). This guide gives step-by-step, copy-pasteable instructions for Phases 5–6.
> Phases 2–4 are kept below for reference (marked DONE). Read the **"Conventions & gotchas"** section
> first — it encodes mistakes already made and solved; repeating them wastes time.

---

## 0. Current state (what is already true)

| Phase | Status | Commit |
|-------|--------|--------|
| Phase 1 — Spring → Micronaut 4 / Java 21 | ✅ DONE | ae4dbf3 |
| Phase 2 — HTTP timeouts, health endpoint, dead code | ✅ DONE | 8b57f2e |
| Phase 3 — Webhook: register, GET handshake, secret auth, self-heal | ✅ DONE | 54cb062 |
| Phase 4 — Multi-stage Dockerfile + docker compose, drop Swarm | ✅ DONE | 2d62709 |
| Phase 5 — Server deploy + Cloudflare Tunnel + Monobank registration | 🔜 **NEXT** | — |
| Phase 6 — (Optional) GraalVM native image | ⬜ optional | — |

- Stack: **Micronaut 4.7 + Java 21**. Build: Maven (`pom.xml`).
- `mvn clean test` is **green**.
- Webhook is fully implemented: controller handles GET + POST, `WebhookRegistrar` registers on startup
  and self-heals every 6 h. Nothing missing code-wise — the remaining work is **server configuration**.
- Branch: `micronaut-webhook-migration`. High-level rationale in `docs/MIGRATION_PLAN.md`.

### Build / test commands (use a Java 21 JDK)

```bash
# A Java 21 JDK must be the active JAVA_HOME. On the dev machine:
export JAVA_HOME=/Users/fyfar/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home

mvn clean test          # compile + run all tests (must stay green after every phase)
mvn -q compile          # compile main only (faster feedback)
mvn package -DskipTests # build the runnable jar -> target/budgetbot-1.0.0.jar
```

---

## Conventions & gotchas (READ THIS — hard-won during Phase 1)

These are **non-obvious** and will bite you if ignored:

1. **Dependency injection: field injection, never Lombok constructors.**
   Micronaut's annotation processor does **not** see Lombok-generated constructors, so
   `@RequiredArgsConstructor` + Micronaut DI fails at runtime (`method 'void <init>()' not found`).
   Pattern used everywhere in this codebase:
   ```java
   @Singleton
   public class Foo {
       @Inject               // jakarta.inject.Inject
       Bar bar;              // package-private (NOT private) so Micronaut injects without reflection
   }
   ```
   Annotations: `jakarta.inject.Singleton`, `jakarta.inject.Inject`. Do **not** reintroduce
   `@RequiredArgsConstructor` on beans.

2. **Micronaut Data + Lombok entities: derived queries do NOT work.**
   Micronaut Data cannot introspect properties of Lombok `@Data` entities, so method-name-derived
   queries (`findById`, `findByAccountId...`) fail at **compile time**
   (`Cannot query entity ... on non-existent property: Id`).
   - Repositories extend **`io.micronaut.data.repository.GenericRepository<E, ID>`** (a no-method root).
   - Every finder is an explicit **`@Query`** (`io.micronaut.data.annotation.Query`, JPQL).
   - CRUD-by-name methods you may declare and rely on: `save` (persist/insert), `update`
     (merge/**upsert**), `deleteAll`, `findAll`. These work because they are not name-derived queries.
   - Repository annotation: `io.micronaut.data.annotation.Repository`.

3. **Never write to the DB from `@PostConstruct`.** The transaction isn't committed there, so the row
   silently never persists. Do DB initialization in a `StartupEvent` listener:
   ```java
   import io.micronaut.context.event.StartupEvent;
   import io.micronaut.runtime.event.annotation.EventListener;
   @EventListener
   public void onStartup(StartupEvent event) { ... }
   ```
   (This is why seeding lives in `ConfigInitializer`, separate from `ConfigService`.)

4. **Entities with an assigned (non-generated) `@Id` must use `update()` (merge) to upsert.**
   `ConfigEntity`'s `@Id` is the `type` enum. `save()` = persist = INSERT → throws a **PK violation**
   the second time. Always use `configRepository.update(...)` for config writes. (BalanceHistoryEntity
   has a generated UUID id, so `save()` is fine there.)

5. **Test classes must be named `*Test`** or Surefire silently skips them. Use
   `@MicronautTest(environments = {"integration", "disableTelegramBot"})`. Mock collaborators with a
   `@MockBean` factory method:
   ```java
   @MockBean(DateTimeRepository.class)
   DateTimeRepository timeRepositoryMock() { return mock(DateTimeRepository.class); }
   ```
   `src/test/java/com/home/budgetbot/test/TestTelegramBotFactory.java` provides a **mock Telegram bot**
   in the `disableTelegramBot` env so `MessageService` (and its dependents) stay creatable.

6. **telegrambots 6.9.x API** (already handled, but for reference): `TelegramLongPollingBot(String token)`
   constructor; `CallbackQuery.getMessage()` returns `MaybeInaccessibleMessage`; `User.getId()` and
   `Contact.getUserId()` return `Long`.

7. **`javax.xml.bind:jaxb-api:2.3.1` is a required dependency.** telegrambots pulls the legacy
   javax-based `jackson-module-jaxb-annotations`; Hibernate's Jackson module discovery loads it and
   needs `javax.xml.bind.annotation.XmlElement` on the classpath. Do not remove this dep.

8. **Config is loaded two ways:**
   - Secret files via `PropertyProvider` from `./data/<name>.json` (relative to the process working
     dir): `monobank.json` (`{"tokenList":[...]}`) and `telegram.json` (`{"login":...,"token":...}`).
   - `application.yaml` for everything else. Env-var overrides use explicit placeholders, e.g.
     `${MONOBANK_WEBHOOK_PUBLIC_URL:}` (default empty). Use this form — Micronaut's implicit
     ENV_VAR→property mapping does not cleanly hit kebab-case keys.

---

## Phase 2 — Resilience hardening ✅ DONE (commit 8b57f2e)

**Goal:** fail-fast HTTP, expose health, remove dead code.

### 2.1 HTTP client timeouts
Edit `src/main/resources/application.yaml`, add under `micronaut:`:
```yaml
micronaut:
  application:
    name: budgetbot
  server:
    port: 7070
  http:
    client:
      read-timeout: 10s
      connect-timeout: 5s
```
This is the direct fix for the original "hung call / timeout" instability (the Feign client had no
timeouts).

### 2.2 Health endpoint
Already enabled in `application.yaml` (`endpoints.health.enabled: true`, `sensitive: false`) and
`micronaut-management` is already a dependency. Endpoint is **`/health`** (NOT `/actuator/health`).
Verify after building: `curl http://localhost:7070/health` → `{"status":"UP"}`.

### 2.3 Delete dead code
- Delete `src/main/java/com/home/budgetbot/bank/event/ClientInfoFailEvent.java` (unused; it belonged to
  the deleted polling scheduler). Confirm no references first: `grep -rn ClientInfoFailEvent src/`.

### 2.4 NON-issue (do NOT "fix")
`BalanceServiceImpl` has `lastBalance.get() == newBalance` where `newBalance` is a primitive `int`.
This unboxes the `Integer` and is **correct**. Do not change it to `.equals()` — the original plan
flagged it but it is safe because one side is a primitive.

### 2.5 Verify & commit
```bash
mvn clean test    # must stay green
git add -A && git commit -m "Phase 2: HTTP client timeouts, health endpoint, remove dead event"
```

---

## Phase 3 — Complete the webhook ✅ DONE (commit 54cb062)

**Goal:** Monobank pushes balance changes to our HTTPS endpoint; we register it, answer its validation
GET, authenticate via a secret path segment, and self-heal if Monobank disables it.

### Monobank webhook contract (facts)
- Register: `POST https://api.monobank.ua/personal/webhook`, header `X-Token: <token>`,
  body `{"webHookUrl":"https://.../path"}`. Rate-limited ~1 request / 60 s per token.
- After registering, Monobank sends a **GET** to that URL — you must return **HTTP 200** or
  registration is rejected.
- On a transaction it sends **POST** `{"type":"StatementItem","data":{"account":"...","statementItem":{...}}}`.
- If you don't return 200 within 5 s it retries at 60 s and 600 s, then **disables the webhook after 3
  failures** (hence the self-healing job below).

### 3.1 Add webhook config to `MonobankProperties`
`src/main/java/com/home/budgetbot/bank/config/MonobankProperties.java`:
```java
package com.home.budgetbot.bank.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("monobank")
public class MonobankProperties {
    private String baseUrl = "https://api.monobank.ua";
    private String webhookPublicUrl;   // e.g. https://budgetbot.example.com  (no trailing slash)
    private String webhookSecret;      // unguessable path segment
}
```

### 3.2 Add config keys to `application.yaml`
```yaml
monobank:
  base-url: ${MONOBANK_BASE_URL:`https://api.monobank.ua`}
  webhook-public-url: ${MONOBANK_WEBHOOK_PUBLIC_URL:}
  webhook-secret: ${MONOBANK_WEBHOOK_SECRET:}
```
(The backticks around the default protect the `://`. Empty default ⇒ registration is skipped when
unset, e.g. in tests.)

### 3.3 New DTO `SetWebhookRequest`
`src/main/java/com/home/budgetbot/bank/client/SetWebhookRequest.java`:
```java
package com.home.budgetbot.bank.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetWebhookRequest {
    @JsonProperty("webHookUrl")
    private String webHookUrl;
}
```

### 3.4 Add `setWebhook` to `MonobankClient`
`src/main/java/com/home/budgetbot/bank/client/MonobankClient.java`:
```java
package com.home.budgetbot.bank.client;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("${monobank.base-url}")
public interface MonobankClient {

    @Get("/personal/client-info")
    ClientInfoDto getClientInfo(@Header("X-Token") String token);

    @Post("/personal/webhook")
    void setWebhook(@Header("X-Token") String token, @Body SetWebhookRequest request);
}
```

### 3.5 Rewrite `WebhookController`: secret path + GET handshake
`src/main/java/com/home/budgetbot/bank/webhook/WebhookController.java`:
```java
package com.home.budgetbot.bank.webhook;

import com.home.budgetbot.bank.config.MonobankProperties;
import com.home.budgetbot.bank.model.BalanceChangedWebhookInput;
import com.home.budgetbot.bank.service.BalanceService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@Controller("/personal/balance/webhook")
public class WebhookController {

    @Inject
    BalanceService balanceService;

    @Inject
    MonobankProperties monobankProperties;

    // Monobank validates the URL with a GET before delivering events; must return 200.
    @Get("/{secret}")
    public HttpResponse<?> validate(@PathVariable String secret) {
        if (!secretMatches(secret)) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok();
    }

    @Post(value = "/{secret}", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<?> balanceChangedWebhook(@PathVariable String secret,
                                                 @Body BalanceChangedWebhookInput input) {
        if (!secretMatches(secret)) {
            return HttpResponse.notFound();
        }
        balanceService.balanceChanged(input);
        return HttpResponse.noContent();
    }

    private boolean secretMatches(String secret) {
        String expected = monobankProperties.getWebhookSecret();
        return expected != null && !expected.isBlank() && Objects.equals(secret, expected);
    }
}
```
Imports note: `io.micronaut.http.annotation.PathVariable`.

### 3.6 New `WebhookRegistrar`: register on startup + self-healing drift check
`src/main/java/com/home/budgetbot/bank/webhook/WebhookRegistrar.java`:
```java
package com.home.budgetbot.bank.webhook;

import com.home.budgetbot.bank.client.ClientInfoDto;
import com.home.budgetbot.bank.client.MonobankClient;
import com.home.budgetbot.bank.client.SetWebhookRequest;
import com.home.budgetbot.bank.config.MonobankProperties;
import com.home.budgetbot.bank.config.MonobankSecretProperties;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@Singleton
public class WebhookRegistrar {

    @Inject
    MonobankClient monobankClient;

    @Inject
    MonobankSecretProperties secretProperties;

    @Inject
    MonobankProperties monobankProperties;

    @EventListener
    public void onStartup(StartupEvent event) {
        register();
    }

    // Monobank disables the webhook after 3 failed deliveries; re-assert it periodically.
    // client-info is rate-limited to ~1/min per token, so 6h is safely conservative.
    @Scheduled(cron = "0 0 */6 * * *")
    public void ensureRegistered() {
        String expected = expectedUrl();
        if (expected == null) {
            return;
        }
        for (String token : secretProperties.getTokenList()) {
            try {
                ClientInfoDto info = monobankClient.getClientInfo(token);
                if (!Objects.equals(expected, info.getWebHookUrl())) {
                    log.warn("Webhook drift detected; re-registering");
                    monobankClient.setWebhook(token, new SetWebhookRequest(expected));
                }
            } catch (Exception e) {
                log.error("Webhook drift check failed", e);
            }
        }
    }

    private void register() {
        String expected = expectedUrl();
        if (expected == null) {
            log.info("monobank.webhook-public-url not set; skipping webhook registration");
            return;
        }
        for (String token : secretProperties.getTokenList()) {
            try {
                monobankClient.setWebhook(token, new SetWebhookRequest(expected));
                log.info("Registered webhook for token {}***",
                        token.substring(0, Math.min(4, token.length())));
            } catch (Exception e) {
                log.error("Failed to register webhook for a token", e);
            }
        }
    }

    /** Full public webhook URL incl. secret, or null if not configured. */
    private String expectedUrl() {
        String base = monobankProperties.getWebhookPublicUrl();
        String secret = monobankProperties.getWebhookSecret();
        if (base == null || base.isBlank() || secret == null || secret.isBlank()) {
            return null;
        }
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return b + "/personal/balance/webhook/" + secret;
    }
}
```
**Why this is test-safe:** with `webhook-public-url`/`webhook-secret` empty (the default, and in the test
env), `expectedUrl()` returns null and no Monobank HTTP call is ever made. The `@Scheduled` cron won't
fire during a short test run.

### 3.7 Tests for Phase 3 (add these)
Create `src/test/java/com/home/budgetbot/bank/webhook/WebhookControllerTest.java`. Use Micronaut's
HTTP client against the running server. Set the secret via the test env. Example:
```java
package com.home.budgetbot.bank.webhook;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(environments = {"integration", "disableTelegramBot"})
class WebhookControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void getWithValidSecretReturns200() {
        // set monobank.webhook-secret in src/test/resources/application-integration.yaml to "test-secret"
        HttpStatus status = client.toBlocking()
                .exchange(HttpRequest.GET("/personal/balance/webhook/test-secret")).status();
        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void getWithWrongSecretReturns404() {
        try {
            client.toBlocking().exchange(HttpRequest.GET("/personal/balance/webhook/nope"));
        } catch (HttpClientResponseException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
    }
}
```
And add to `src/test/resources/application-integration.yaml`:
```yaml
monobank:
  base-url: "http://localhost:8080"
  webhook-secret: "test-secret"
```
(Leave `webhook-public-url` unset so the registrar stays inert during tests.)

### 3.8 Verify & commit
```bash
mvn clean test
git add -A && git commit -m "Phase 3: complete Monobank webhook (register, GET handshake, secret auth, self-heal)"
```

---

## Phase 4 — Dockerize (single docker compose, no Swarm) ✅ DONE (commit 2d62709)

**Goal:** one-command, reproducible deploy of the app + Cloudflare tunnel on the 1 GB box.

### 4.1 Multi-stage `Dockerfile` (replace the existing one)
```dockerfile
# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/budgetbot-1.0.0.jar /app/app.jar
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```
(`curl` is installed only for the compose healthcheck. Confirm the jar name with `ls target/` after a
local `mvn package` — it should be `budgetbot-1.0.0.jar`.)

### 4.2 `docker-compose.yml` (new, at repo root)
```yaml
services:
  bot:
    build: .
    restart: always
    mem_limit: 384m
    environment:
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=65.0"
      MONOBANK_WEBHOOK_PUBLIC_URL: "${MONOBANK_WEBHOOK_PUBLIC_URL}"
      MONOBANK_WEBHOOK_SECRET: "${MONOBANK_WEBHOOK_SECRET}"
      TZ: "Europe/Kyiv"
    volumes:
      - ./data:/app/data          # H2 db + monobank.json + telegram.json live here
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:7070/health"]
      interval: 30s
      timeout: 5s
      retries: 3

  cloudflared:
    image: cloudflare/cloudflared:latest
    restart: always
    command: tunnel run
    environment:
      TUNNEL_TOKEN: "${TUNNEL_TOKEN}"
    depends_on:
      - bot
```
**Key points:**
- The `bot` service has **no `ports:`** — it is never exposed to the host. `cloudflared` reaches it over
  the private compose network at `http://bot:7070` (configured in the Cloudflare dashboard, Phase 5).
- `./data` bind mount: app reads `./data/*.json` and `./data/database*` relative to WORKDIR `/app`, so
  host `./data` ⇒ container `/app/data`. Put `monobank.json` and `telegram.json` there on the server.

### 4.3 Secrets via `.env` (gitignored)
Create `.env.example` (committed) and `.env` (NOT committed):
```
# .env.example
TUNNEL_TOKEN=your-cloudflare-named-tunnel-token
MONOBANK_WEBHOOK_PUBLIC_URL=https://budgetbot.yourdomain.com
MONOBANK_WEBHOOK_SECRET=replace-with-long-random-string
```
Append to `.gitignore`:
```
.env
```
(`data` is already gitignored.)

### 4.4 Update `README.md`
Replace the "Deploy" / docker-stack/swarm section with:
```markdown
## Deploy

1. Put `monobank.json` (`{"tokenList":["<token>"]}`) and `telegram.json`
   (`{"login":"<bot>","token":"<bot-token>"}`) into `./data/`.
2. Copy `.env.example` to `.env` and fill in TUNNEL_TOKEN, MONOBANK_WEBHOOK_PUBLIC_URL,
   MONOBANK_WEBHOOK_SECRET.
3. `docker compose up -d --build`
4. Logs: `docker compose logs -f`. Health: `docker compose exec bot curl -fsS http://localhost:7070/health`.
```

### 4.5 Verify & commit
```bash
mvn package -DskipTests           # confirm jar builds and name
docker compose build              # confirm image builds
git add -A && git commit -m "Phase 4: multi-stage Dockerfile + docker compose (bot + cloudflared), drop swarm"
```

---

## Phase 5 — Server Setup + Cloudflare Tunnel + Monobank Registration 🔜 NEXT

This phase is entirely **infrastructure / operations** — no code changes needed. Everything runs on your
Oracle AMD Ubuntu server.

### Overview of what we're building

```
Monobank → HTTPS → Cloudflare Edge → Cloudflare Tunnel (outbound) → cloudflared container
                                                                             ↓ (Docker network)
                                                                         bot container :7070
```

No inbound ports on Oracle. No firewall changes needed. `cloudflared` makes an **outbound** connection
to Cloudflare and keeps a persistent tunnel open.

---

### 5.1 Server hygiene (one-time, do first)

SSH into the Oracle box and run:

```bash
# ── 2 GB swap (OOM insurance on the 1 GB Micro) ──────────────────────────
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# ── Reduce swap aggressiveness (Linux defaults to 60, which thrashes disk) ──
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swappiness.conf
sudo sysctl --system

# ── Verify swap is active ─────────────────────────────────────────────────
free -h   # should show ~2G under "Swap"
```

If the old Docker Swarm deployment is still running, tear it down first:
```bash
docker stack rm budgetbot 2>/dev/null || true
docker swarm leave --force 2>/dev/null || true
```

---

### 5.2 Install Docker Engine on Ubuntu Oracle

> **Skip this section if Docker is already installed.** Check with: `docker --version`.

```bash
# ── Install prerequisites ────────────────────────────────────────────────
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# ── Add Docker's official GPG key ────────────────────────────────────────
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# ── Add the Docker apt repository ────────────────────────────────────────
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# ── Install Docker Engine + Compose plugin ───────────────────────────────
sudo apt-get update
sudo apt-get install -y \
  docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin

# ── Start Docker and enable on boot ──────────────────────────────────────
sudo systemctl start docker
sudo systemctl enable docker

# ── Allow your user to run Docker without sudo ───────────────────────────
sudo usermod -aG docker $USER
# ⚠ Log out and back in for group membership to take effect, OR run:
newgrp docker

# ── Verify ───────────────────────────────────────────────────────────────
docker --version          # e.g. Docker version 26.x.x
docker compose version    # e.g. Docker Compose version v2.x.x
docker run --rm hello-world
```

> **Oracle-specific note:** Docker is installed with iptables integration. Since the `bot` service has
> **no published ports** (`ports:` is absent from `docker-compose.yml`), Oracle's Security Lists and
> firewall rules need **zero changes** — all traffic is outbound-initiated by `cloudflared`.

---

### 5.3 Clone the repo on the server

```bash
# Pick a directory, e.g. /opt/budgetbot or ~/budgetbot
git clone https://github.com/<your-org>/budgetbot.git ~/budgetbot
cd ~/budgetbot
git checkout micronaut-webhook-migration   # or main after you merge
```

If you don't have git-based access, transfer with scp:
```bash
# From your local machine:
scp -r /path/to/budgetbot ubuntu@<server-ip>:~/budgetbot
```

---

### 5.4 Create the Cloudflare Tunnel (dashboard walkthrough)

This is the **token-based / remote-managed** approach — tunnel config is stored in Cloudflare's cloud,
the connector just authenticates with a token. No `config.yml` needed.

#### Step A — Create a named tunnel
1. Open **Cloudflare Zero Trust** dashboard: `one.dash.cloudflare.com`
2. In the left sidebar: **Networks → Tunnels**
3. Click **"Create a tunnel"**
4. Connector type: **Cloudflared** → Continue
5. Name the tunnel: `budgetbot` (or any name) → Save tunnel

#### Step B — Get the tunnel token
On the "Install and run a connector" screen you will see something like:
```
cloudflared service install eyJhIjoiXXXX...
```
The long base64 string after `install` is your **TUNNEL_TOKEN**. Copy it — you'll need it in step 5.6.

> Do NOT close this screen yet; you still need to configure the public hostname.

#### Step C — Add a public hostname
1. Still on the tunnel creation flow, click **"Next"** to reach the **Public Hostname** tab
   (or navigate to the tunnel → **Edit** → **Public Hostname** tab after saving)
2. Click **"Add a public hostname"**
3. Fill in:
   | Field | Value |
   |-------|-------|
   | Subdomain | `budgetbot` |
   | Domain | `yourdomain.com` (must be in your Cloudflare account) |
   | Service Type | `HTTP` |
   | URL | `bot:7070` |
4. Click **Save hostname**

The DNS CNAME (`budgetbot.yourdomain.com → <tunnel-id>.cfargotunnel.com`) is created **automatically**.

> **Why `bot:7070`?** When `cloudflared` runs in the Docker Compose network alongside the `bot` service,
> Docker's internal DNS resolves the hostname `bot` to the bot container's IP. Cloudflare stores this as
> the destination for all requests forwarded through the tunnel.

#### Step D — (Optional) Enable HTTP/2 and compression
In the tunnel's **Settings** → enable **HTTP/2** for the origin. This slightly reduces overhead on the
1 GB box.

---

### 5.5 Create the `.env` file on the server

```bash
cd ~/budgetbot

# Generate a secure random webhook secret (do this once; save it somewhere safe)
WEBHOOK_SECRET=$(openssl rand -hex 32)
echo "Generated secret: $WEBHOOK_SECRET"

# Copy the example and edit it
cp .env.example .env
nano .env   # or vim .env
```

Fill in `.env`:
```dotenv
TUNNEL_TOKEN=eyJhIjoiXXXX...          # the token from Step B above
MONOBANK_WEBHOOK_PUBLIC_URL=https://budgetbot.yourdomain.com
MONOBANK_WEBHOOK_SECRET=<output of openssl rand -hex 32>
```

> `.env` is gitignored. Never commit it. Back it up somewhere outside the repo (e.g. a password manager).

---

### 5.6 Create the `data/` directory and config files

The bot reads `./data/monobank.json` and `./data/telegram.json` at startup. These files hold your API
tokens and are **not** in the repo (gitignored).

```bash
mkdir -p ~/budgetbot/data

# ── monobank.json ─────────────────────────────────────────────────────────
cat > ~/budgetbot/data/monobank.json << 'EOF'
{
  "tokenList": [
    "your-monobank-api-token-here"
  ]
}
EOF

# ── telegram.json ─────────────────────────────────────────────────────────
cat > ~/budgetbot/data/telegram.json << 'EOF'
{
  "login": "YourBotUsername",
  "token": "123456789:ABCdef..."
}
EOF

# ── Lock down permissions ─────────────────────────────────────────────────
chmod 600 ~/budgetbot/data/monobank.json
chmod 600 ~/budgetbot/data/telegram.json
```

> **Monobank API token:** get it at `https://api.monobank.ua` (create a new token; the same one you
> used with the old bot works fine).
> **Telegram bot token:** from `@BotFather` on Telegram (same token as before).

---

### 5.7 Build and start the stack

```bash
cd ~/budgetbot

# Build the bot image (downloads Maven dependencies on first run — takes ~5 min)
docker compose build

# Start everything
docker compose up -d

# Watch the logs (Ctrl+C to stop following, containers keep running)
docker compose logs -f
```

Expected log output within ~30 seconds:
```
cloudflared  | ... INF Connection established connIndex=0
cloudflared  | ... INF Connection established connIndex=1
bot          | ... Micronaut application started
bot          | ... Registered webhook for token u3Ap***
```

> **Build time note:** The first `docker compose build` on the 1 GB box compiles the full Maven project
> inside the container. This can take 5–10 minutes due to dependency downloads and the burstable CPU.
> Subsequent builds (after code changes) are faster because the Docker layer cache retains downloaded
> dependencies.

---

### 5.8 Verify Cloudflare tunnel is connected

```bash
docker compose logs cloudflared | grep -E "connection|healthy|error"
```

Expected: lines containing `Connection established` (cloudflared opens 4 connections by default).

In the Cloudflare Zero Trust dashboard:
- **Networks → Tunnels** → your tunnel should show **HEALTHY** (green dot).
- **Networks → Tunnels → your tunnel → Public Hostname** → `budgetbot.yourdomain.com` should show active.

---

### 5.9 Verify the bot health endpoint

```bash
# From the server (inside the compose network):
docker compose exec bot curl -fsS http://localhost:7070/health
# → {"status":"UP"}

# From outside (via the Cloudflare Tunnel):
curl -fsS https://budgetbot.yourdomain.com/health
# → {"status":"UP"}
```

If the second command fails, the tunnel is not routing correctly. Check:
1. `docker compose logs cloudflared` for errors
2. The public hostname URL in the Cloudflare dashboard is `bot:7070` (not `localhost:7070`)
3. The domain is on Cloudflare's nameservers (DNS → check `budgetbot.yourdomain.com` CNAME exists)

---

### 5.10 Verify the Monobank webhook endpoint

```bash
# Load the secret from your .env
source ~/budgetbot/.env

# GET handshake — Monobank validates this before delivering events
curl -fsS "https://budgetbot.yourdomain.com/personal/balance/webhook/${MONOBANK_WEBHOOK_SECRET}"
# → HTTP 200 (empty body or {})

# Wrong secret → must return 404
curl -o /dev/null -w "%{http_code}" \
  "https://budgetbot.yourdomain.com/personal/balance/webhook/wrongsecret"
# → 404
```

---

### 5.11 Verify automatic webhook registration

The app calls `WebhookRegistrar.onStartup` at startup to register the webhook URL with Monobank.
Check the logs:

```bash
docker compose logs bot | grep -i webhook
```

Expected:
```
INFO  WebhookRegistrar - Registered webhook for token u3Ap***
```

If you see an error (e.g. rate-limit, 422), wait 60 seconds and trigger re-registration manually:

```bash
source ~/budgetbot/.env

curl -X POST https://api.monobank.ua/personal/webhook \
  -H "X-Token: $(cat ~/budgetbot/data/monobank.json | python3 -c "import sys,json; print(json.load(sys.stdin)['tokenList'][0])")" \
  -H "Content-Type: application/json" \
  -d "{\"webHookUrl\":\"${MONOBANK_WEBHOOK_PUBLIC_URL}/personal/balance/webhook/${MONOBANK_WEBHOOK_SECRET}\"}"
# → HTTP 200, empty body = success
```

---

### 5.12 End-to-end test

Make a **1 UAH card-to-card transfer** or any small real transaction on the registered Monobank account.
Within a few seconds you should receive a Telegram message from the bot with the balance update.

```bash
# Watch live for the event arriving:
docker compose logs -f bot | grep -E "balanceChanged|StatementItem|webhook"
```

---

### 5.13 Verify memory headroom

```bash
free -h          # Swap should show ~2G; used RAM should be well under 1G
docker stats --no-stream
```

Expected RSS:
| Container | Expected |
|-----------|----------|
| `budgetbot-bot-1` | ~120–200 MB |
| `budgetbot-cloudflared-1` | ~30–50 MB |

Total ~150–250 MB of a 1 GB box = comfortable headroom. If the bot exceeds 384 MB (its `mem_limit`),
Docker will OOM-kill it and restart — the logs will show `OOMKilled`. If that happens, either reduce
logging verbosity or increase `mem_limit` slightly in `docker-compose.yml`.

---

### 5.14 Verify self-healing (optional, on next restart)

The `WebhookRegistrar` runs a drift-check cron every 6 hours. To test it manually:

```bash
# Restart the bot; the startup registrar fires again
docker compose restart bot
docker compose logs bot | grep -i "webhook"
# Should see "Registered webhook" again
```

---

### 5.15 Troubleshooting quick-reference

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `cloudflared` logs: `Unable to reach the origin service` | `bot` container not yet started or not healthy | `docker compose ps` — wait for bot to be healthy |
| Tunnel dashboard shows **DOWN** | Token expired or wrong | Re-copy token from dashboard → update `.env` → `docker compose up -d` |
| `curl https://...` hangs / TLS error | Domain not on Cloudflare or CNAME missing | Check `dig budgetbot.yourdomain.com` — should CNAME to cfargotunnel.com |
| Webhook GET → 404 (correct secret) | `MONOBANK_WEBHOOK_SECRET` env var not loaded | `docker compose exec bot env \| grep MONO` — check it matches `.env` |
| Bot OOM-killed | Memory too low | Increase `mem_limit` in `docker-compose.yml` to `448m` or `512m` |
| `docker compose build` very slow | Maven downloads on burstable CPU | Normal on first build; subsequent builds use layer cache |

---

## Phase 6 — (Optional) GraalVM native image

Low-risk on Micronaut. Only attempt after the JVM build is stable in production.
```bash
mvn package -Dpackaging=native-image   # requires a GraalVM/Mandrel 21 JDK
```
Then change the Dockerfile runtime stage to a small base (e.g. `debian:stable-slim` or distroless) and
copy the native binary instead of the jar. Target ~84 MB RSS / ~50 ms start. Keep the JVM image as a
fallback. (Reflection config may be needed for Jackson DTOs and telegrambots; add
`@io.micronaut.core.annotation.ReflectiveAccess` / `@Serdeable` or `src/main/resources/META-INF/native-image`
hints as the native build reports missing classes.)

---

## Definition of done (per phase)
- `mvn clean test` green.
- Changes committed on `micronaut-webhook-migration` with a `Phase N: ...` message.
- Phase 5 additionally: live webhook delivers a Telegram message for a real transaction.
