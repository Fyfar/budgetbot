# BudgetBot — Oracle Ubuntu 24.04 Server Setup

Step-by-step guide for a brand-new VM.Standard.E2.1.Micro (1 GB RAM, Ubuntu 24.04 Minimal).
No Docker, no Cloudflare, no nothing — starting from zero.

```
Monobank → HTTPS → Cloudflare Edge → Cloudflare Tunnel (outbound) → cloudflared container
                                                                             ↓ (Docker network)
                                                                         bot container :7070
```

No inbound ports opened on Oracle. No firewall changes needed. `cloudflared` makes an **outbound**
connection to Cloudflare — Oracle's Security Lists and host iptables need zero changes.

---

## Stage 1 — First login, verify the machine

```bash
ssh ubuntu@<your-server-ip>

cat /etc/os-release | grep -E "NAME|VERSION"
free -h
df -h /
```

Expected: Ubuntu 24.04, ~1 GB RAM, ~40–47 GB disk.

---

## Stage 2 — Swap + kernel tuning (OOM protection)

```bash
# 2 GB swapfile
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# reduce swap aggressiveness (default 60 thrashes disk; 10 is sensible)
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swappiness.conf
sudo sysctl --system

# verify — Swap row should show ~2.0G
free -h
```

---

## Stage 3 — Install Docker Engine

```bash
# base packages (Minimal may be missing some)
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg git

# add Docker's GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# add Docker apt repo (noble = Ubuntu 24.04 codename)
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# install Engine + Compose plugin
sudo apt-get update
sudo apt-get install -y \
  docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin

# start + enable on boot
sudo systemctl enable --now docker

# add ubuntu user to docker group (no sudo needed after this)
sudo usermod -aG docker ubuntu
newgrp docker

# verify — should see hello-world output
docker --version
docker compose version
docker run --rm hello-world
```

---

## Stage 4 — Clone the repo

```bash
git clone https://github.com/<your-org>/budgetbot.git ~/budgetbot
cd ~/budgetbot
git checkout micronaut-webhook-migration

ls   # should see: Dockerfile  docker-compose.yml  pom.xml  src  .env.example
```

For a private repo: use a GitHub personal access token (`https://<token>@github.com/...`) or
set up an SSH deploy key before cloning.

---

## Stage 5 — Create the Cloudflare Tunnel (browser)

Do this in your browser before touching the server again:

1. Open **Cloudflare Zero Trust** → **Networks → Tunnels** → **Create a tunnel**
2. Connector type: **Cloudflared** → Continue
3. Name: `budgetbot` → Save tunnel
4. On the "Install connector" screen copy the long token from the command shown:
   ```
   cloudflared service install eyJhIjoiXXXXXXXX...
   ```
   Everything after `install ` is your `TUNNEL_TOKEN`.
5. Click **Next** → **Public Hostname** tab → **Add a public hostname**:

   | Field | Value |
   |-------|-------|
   | Subdomain | `budgetbot` |
   | Domain | `yourdomain.com` |
   | Service Type | `HTTP` |
   | URL | `bot:7070` |

6. Save. Cloudflare auto-creates the DNS CNAME.

> `bot:7070` is the internal Docker Compose service name. The `cloudflared` container resolves it
> via Docker's internal DNS — this is why the bot has no published ports.

---

## Stage 6 — Create `.env` on the server

```bash
cd ~/budgetbot

# generate a secure random webhook secret
WEBHOOK_SECRET=$(openssl rand -hex 32)
echo "Webhook secret (save this): $WEBHOOK_SECRET"

cat > .env << EOF
TUNNEL_TOKEN=paste-your-cloudflare-token-here
MONOBANK_WEBHOOK_PUBLIC_URL=https://budgetbot.yourdomain.com
MONOBANK_WEBHOOK_SECRET=${WEBHOOK_SECRET}
EOF

chmod 600 .env
cat .env   # verify
```

> `.env` is gitignored. Back up the secret and token somewhere safe (password manager).

---

## Stage 7 — Create `data/` config files

```bash
cd ~/budgetbot
mkdir -p data

# monobank.json — API token from https://api.monobank.ua
cat > data/monobank.json << 'EOF'
{
  "tokenList": [
    "your-monobank-api-token-here"
  ]
}
EOF

# telegram.json — bot credentials from @BotFather
cat > data/telegram.json << 'EOF'
{
  "login": "YourBotUsername",
  "token": "123456789:ABCdef..."
}
EOF

chmod 600 data/monobank.json data/telegram.json
```

---

## Stage 8 — Build and start

```bash
cd ~/budgetbot

# build the image
# first run downloads Maven deps — takes ~5–10 min on the burstable CPU
docker compose build

# start both containers in the background
docker compose up -d

# follow logs (Ctrl+C to stop following; containers keep running)
docker compose logs -f
```

**What to look for in the logs:**

```
cloudflared | ... INF Connection established connIndex=0   ← tunnel up (appears ×4)
bot         | ... Micronaut application started             ← app running
bot         | ... Registered webhook for token u3Ap***     ← Monobank webhook registered
```

---

## Stage 9 — Verify end-to-end

```bash
# 1. health check (internal, inside compose network)
docker compose exec bot curl -fsS http://localhost:7070/health
# → {"status":"UP"}

# 2. health check via Cloudflare Tunnel (run from server or your laptop)
curl -fsS https://budgetbot.yourdomain.com/health
# → {"status":"UP"}

# 3. webhook GET handshake — Monobank hits this to validate the URL
source ~/budgetbot/.env
curl -fsS "https://budgetbot.yourdomain.com/personal/balance/webhook/${MONOBANK_WEBHOOK_SECRET}"
# → HTTP 200

# 4. wrong secret → must return 404
curl -o /dev/null -w "%{http_code}\n" \
  https://budgetbot.yourdomain.com/personal/balance/webhook/wrongsecret
# → 404

# 5. memory check — bot ~120–200 MB, cloudflared ~30–50 MB
free -h
docker stats --no-stream
```

---

## Stage 10 — Real transaction test

Make a **1 UAH** card-to-card transfer on the Monobank account linked to the registered token.
Within a few seconds you should receive a Telegram message from the bot.

Watch live:
```bash
docker compose logs -f bot | grep -E "balance|webhook|Statement"
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `cloudflared` logs: `Unable to reach origin` | `bot` not yet healthy | `docker compose ps` — wait for healthy status |
| Tunnel dashboard shows DOWN | Token wrong or expired | Re-copy token from dashboard → update `.env` → `docker compose up -d` |
| `curl https://...` hangs | Domain not on Cloudflare or CNAME missing | `dig budgetbot.yourdomain.com` — should CNAME to `cfargotunnel.com` |
| Webhook GET → 404 with correct secret | Env var not loaded into container | `docker compose exec bot env \| grep MONO` — compare with `.env` |
| Bot OOM-killed, restarts | Hitting 384 MB mem_limit | Increase `mem_limit` to `448m` in `docker-compose.yml` |
| `docker compose build` very slow | Maven dependency download on burstable CPU | Normal on first build; layer cache speeds up subsequent builds |
| Webhook not registered at startup | Rate-limited (1 req/60s per token) | Wait 60s; registrar retries automatically every 6h, or register manually (see below) |

### Manual webhook registration

If the startup registrar fails due to rate-limiting:

```bash
source ~/budgetbot/.env
TOKEN=$(python3 -c "import json; print(json.load(open('data/monobank.json'))['tokenList'][0])")

curl -X POST https://api.monobank.ua/personal/webhook \
  -H "X-Token: $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"webHookUrl\":\"${MONOBANK_WEBHOOK_PUBLIC_URL}/personal/balance/webhook/${MONOBANK_WEBHOOK_SECRET}\"}"
# HTTP 200 empty body = success
```

---

## Useful day-to-day commands

```bash
# check what's running
docker compose ps

# view recent logs
docker compose logs --tail=100 bot
docker compose logs --tail=50 cloudflared

# restart just the bot (e.g. after a config change)
docker compose restart bot

# pull a new image and redeploy
git pull
docker compose build && docker compose up -d

# stop everything
docker compose down

# hard stop + remove volumes (destructive — removes H2 DB!)
# docker compose down -v
```
