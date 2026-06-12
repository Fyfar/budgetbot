# BudgetBot

Telegram bot that tracks Monobank balance changes and sends daily budget reports.

## Links
- [Monobank API](https://api.monobank.ua/docs/)
- [BotFather](https://t.me/BotFather)

## Deploy

### 1. Prepare config files

Create `./data/monobank.json`:
```json
{
  "tokenList": ["your_monobank_token"]
}
```

Create `./data/telegram.json`:
```json
{
  "login": "your_bot_username",
  "token": "your_bot_token"
}
```

### 2. Configure secrets

```bash
cp .env.example .env
# Edit .env: set TUNNEL_TOKEN, MONOBANK_WEBHOOK_PUBLIC_URL, MONOBANK_WEBHOOK_SECRET
```

`MONOBANK_WEBHOOK_SECRET` should be a long random string (e.g. `openssl rand -hex 32`).

### 3. Set up Cloudflare Tunnel

In [Cloudflare Zero Trust](https://one.dash.cloudflare.com/) → Networks → Tunnels:
1. Create a named tunnel, copy the **token** → `TUNNEL_TOKEN` in `.env`
2. Add public hostname: `budgetbot.yourdomain.com` → service type `HTTP`, URL `bot:7070`
3. Set `MONOBANK_WEBHOOK_PUBLIC_URL=https://budgetbot.yourdomain.com` in `.env`

### 4. Start

```bash
docker compose up -d --build
```

Verify:
```bash
# Health check
docker compose exec bot curl -fsS http://localhost:7070/health

# Webhook handshake (should return 200)
curl https://budgetbot.yourdomain.com/personal/balance/webhook/<your-secret>

# Logs
docker compose logs -f
```

The app registers the webhook with Monobank automatically on startup. A 6-hourly cron
re-registers it if Monobank disables it after failed deliveries.

## Development

```bash
# Java 21 required
mvn clean test          # run tests
mvn package -DskipTests # build jar → target/budgetbot-1.0.0.jar
```
