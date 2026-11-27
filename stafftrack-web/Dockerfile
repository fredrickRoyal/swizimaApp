# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Build Angular 22 Staff Out-of-Office Portal (stafftrack-web)
# =============================================================================
FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .
RUN npm run build

# =============================================================================
# Stage 2: Serve via Nginx with Dynamic Gateway Discovery & Reverse Proxy
# =============================================================================
FROM nginx:stable-alpine

COPY --from=builder /app/dist/stafftrack-web/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Auto-detect reachable API Gateway at container startup (overrides broken hairpin-NAT public IPs
# from older host docker-compose.yml files and selects the container's real DNS resolver).
RUN cat <<'EOF' > /docker-entrypoint.d/30-auto-resolve-gateway.sh && chmod +x /docker-entrypoint.d/30-auto-resolve-gateway.sh
#!/bin/sh
set -e

probe_url() {
  url="$1"
  hostport="${url#http://}"
  hostport="${hostport#https://}"
  hostport="${hostport%%/*}"
  host="${hostport%%:*}"
  port="${hostport##*:}"
  if [ "$port" = "$host" ] || [ -z "$port" ]; then
    case "$url" in
      https://*) port=443 ;;
      *) port=80 ;;
    esac
  fi
  nc -z -w 2 "$host" "$port" >/dev/null 2>&1
}

# Compute Linux Docker bridge host IP from /proc/net/route (e.g. 172.17.0.1 / 172.18.0.1)
BRIDGE_GW=$(awk '$2 == "00000000" {
  printf "%d.%d.%d.%d\n", "0x" substr($3,7,2), "0x" substr($3,5,2), "0x" substr($3,3,2), "0x" substr($3,1,2);
  exit
}' /proc/net/route 2>/dev/null || true)

CHOSEN=""
PROBE_STATUS="unreachable"

for candidate in \
  "http://swizima-apigateway:9080" \
  "http://apigateway:9080" \
  "${API_GATEWAY_URL}" \
  ${BRIDGE_GW:+"http://${BRIDGE_GW}:9080"} \
  "http://host.docker.internal:9080" \
  "http://127.0.0.1:9080"
do
  [ -z "$candidate" ] && continue
  clean="${candidate%/}"
  if probe_url "$clean"; then
    CHOSEN="$clean"
    PROBE_STATUS="connected"
    break
  fi
done

if [ -z "$CHOSEN" ]; then
  CHOSEN="${API_GATEWAY_URL:-http://swizima-apigateway:9080}"
  CHOSEN="${CHOSEN%/}"
fi

DNS_RESOLVER=$(awk '/^nameserver/ {print $2; exit}' /etc/resolv.conf 2>/dev/null || true)
[ -z "$DNS_RESOLVER" ] && DNS_RESOLVER="127.0.0.11"
# Wrap IPv6 nameserver in brackets for Nginx resolver directive
case "$DNS_RESOLVER" in
  *:*) DNS_RESOLVER="[$DNS_RESOLVER]" ;;
esac

echo "[stafftrack-web] Selected API Gateway upstream: ${CHOSEN} (${PROBE_STATUS}), resolver=${DNS_RESOLVER}"

export API_GATEWAY_URL="$CHOSEN"
export NGINX_DNS_RESOLVER="$DNS_RESOLVER"
export GATEWAY_PROBE_STATUS="$PROBE_STATUS"

envsubst '${API_GATEWAY_URL} ${NGINX_DNS_RESOLVER} ${GATEWAY_PROBE_STATUS}' \
  < /etc/nginx/templates/default.conf.template \
  > /etc/nginx/conf.d/default.conf
EOF

ENV NGINX_ENVSUBST_FILTER=^(API_GATEWAY_URL|NGINX_DNS_RESOLVER|GATEWAY_PROBE_STATUS)$
ENV API_GATEWAY_URL=http://swizima-apigateway:9080
ENV NGINX_DNS_RESOLVER=127.0.0.11
ENV GATEWAY_PROBE_STATUS=init

EXPOSE 80
