# Swizima Standalone WhatsApp Relay Service (`whatsappapi`)

Stateless Spring Boot 4 / Java 21 microservice designed to run on **DigitalOcean** (`165.245.157.109`) so Uganda-hosted services (`stafftrackapi`, `usersapi`) can send WhatsApp templates, Out-of-Office / Report-Back supervisor notifications, and login OTPs without hitting ISP restrictions on `graph.facebook.com`.

## Meta Webhook Configuration (Meta Developer Console)

Meta requires **HTTPS on port `443`** with a valid TLS certificate for WhatsApp Webhooks. The included `caddy` container in `docker-compose.yml` automatically obtains a free Let's Encrypt certificate for `swizima.com` (ensure TCP ports `80` and `443` are open in DigitalOcean Firewall / UFW):

- **Callback URL**: `https://swizima.com/api/v1/whatsapp/webhook`
  *(or set `WEBHOOK_DOMAIN=whatsapp.yourdomain.com` for `https://whatsapp.yourdomain.com/api/v1/whatsapp/webhook`)*
- **Verify Token**: `swz_wa_whsec_9f4c2e8a71b6403d8e5f1a2c9b7d4e60`
- **Webhook Fields to Subscribe**: `messages`

## Build & Deploy

```bash
# 1. From repo root: build & push whatsappapi
docker buildx bake whatsappapi --push

# 2. On DigitalOcean Droplet (165.245.157.109) with docker-compose.yml:
sudo ufw allow 80/tcp && sudo ufw allow 443/tcp || true
docker compose pull
docker compose up -d
```

## Quick Tests

```bash
# 1. Test Webhook Verification (same handshake Meta sends)
curl -i "https://swizima.com/api/v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=swz_wa_whsec_9f4c2e8a71b6403d8e5f1a2c9b7d4e60&hub.challenge=123456789"

# 2. Send Test WhatsApp Message (uses 3p_direct_integration_test_template -> 256700408387)
curl -i -X POST "https://swizima.com/api/v1/whatsapp/test" \
  -H "X-API-Key: swz-wa-relay-2026-secret"

# 3. Inspect Recent Incoming Meta Webhook Events
curl -i "https://swizima.com/api/v1/whatsapp/webhook-events" \
  -H "X-API-Key: swz-wa-relay-2026-secret"
```
