# Links
- [Monobank API](https://api.monobank.ua/docs/)
- [Bot Father](https://t.me/BotFather)

# Deploy

### Create docker config

Monobank config file example:
```json
{
  "tokenList": [
    "your_token"
  ]
}
```

```bash
> docker config create budgetbot_monobank.json YOUR_FILE_NAME
```

Telegram config file example:
```json
{
  "login": "your_bot_username",
  "token": "your_bot_token"
}
```

```bash
> docker config create budgetbot_telegram.json YOUR_FILE_NAME
```

### Create docker volume to store database

```bash
> docker volume create --name=budgetbot-data
```

### Deploy stack
```yaml
version: '3.3'
services:
  bot:
    image: dassader/budgetbot
    restart: always
    volumes:
      - "budgetbot-data:/data"
    configs:
      - source: budgetbot_monobank.json
        target: /data/monobank.json
      - source: budgetbot_telegram.json
        target: /data/telegram.json
    deploy:
      mode: global
    environment:
      TZ: Europe/Kiev

volumes:
  budgetbot-data:
    external: true

configs:
  budgetbot_monobank.json:
    external: true
  budgetbot_telegram.json:
    external: true
```

```bash
> docker stack deploy --compose-file=docker-compose.yaml YOUR_STACK_NAME
```