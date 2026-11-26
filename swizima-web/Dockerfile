# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Build Angular 22 Web Portal (swizima-web)
# =============================================================================
FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .
RUN npm run build

# =============================================================================
# Stage 2: Serve via Nginx & proxy /api/ to the API Gateway
# =============================================================================
FROM nginx:stable-alpine
COPY --from=builder /app/dist/swizima-web/browser /usr/share/nginx/html

RUN mkdir -p /etc/nginx/templates && \
    cat <<'EOF' > /etc/nginx/templates/default.conf.template
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Use Docker's internal DNS resolver so Nginx doesn't crash if apigateway isn't up yet
    resolver 127.0.0.11 valid=30s ipv6=off;

    # Proxy /api requests to the Swizima API Gateway
    location /api/ {
        set $upstream ${API_GATEWAY_URL};
        proxy_pass $upstream;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header Origin "";
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Prevent caching of runtime environment config
    location = /env.js {
        add_header Cache-Control "no-store, no-cache, must-revalidate";
    }

    # Angular SPA fallback routing
    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

# Generate /usr/share/nginx/html/env.js from environment variables on container startup
RUN cat <<'EOF' > /docker-entrypoint.d/30-generate-env-js.sh && chmod +x /docker-entrypoint.d/30-generate-env-js.sh
#!/bin/sh
set -e
cat <<EOT > /usr/share/nginx/html/env.js
window.__SWIZIMA_ENV__ = {
  mobileAppDownloadUrl: "${MOBILE_APP_DOWNLOAD_URL}"
};
EOT
EOF

# Only substitute ${API_GATEWAY_URL} in Nginx conf so Nginx variables ($host, $uri, $upstream, etc.) remain intact
ENV NGINX_ENVSUBST_FILTER=API_GATEWAY_URL
ENV API_GATEWAY_URL=http://swizima-apigateway:9080
ENV MOBILE_APP_DOWNLOAD_URL=https://drive.google.com/drive/folders/1cLdYhVLrO4Oyh1xLOhFIJIWCW-99WSg4?usp=sharing
EXPOSE 80
