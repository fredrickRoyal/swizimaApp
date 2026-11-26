# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Build Angular 22 SOOTR Web Portal (stafftrack-web)
# =============================================================================
FROM node:24-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .
RUN npm run build

# =============================================================================
# Stage 2: Serve via Nginx with SPA Routing & Reverse Proxy Support
# =============================================================================
FROM nginx:stable-alpine

COPY --from=builder /app/dist/stafftrack-web/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Only substitute ${API_GATEWAY_URL} in nginx.conf so Nginx runtime variables ($host, $uri, $upstream, etc.) remain intact
ENV NGINX_ENVSUBST_FILTER=API_GATEWAY_URL
ENV API_GATEWAY_URL=http://154.72.196.32:9080

EXPOSE 80

