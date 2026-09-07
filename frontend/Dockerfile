# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* ./
# The stock npm retry/timeout settings give up on a slow or flaky registry
# leg with EIDLETIMEOUT, which fails an unattended deploy for no code reason.
RUN npm config set fetch-retries 5 \
    && npm config set fetch-retry-mintimeout 20000 \
    && npm config set fetch-retry-maxtimeout 120000 \
    && npm config set fetch-timeout 600000 \
    && npm ci --include=dev --prefer-offline --no-audit --no-fund
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN adduser -D -g '' appuser && chown -R appuser:appuser /usr/share/nginx/html /var/cache/nginx /var/log/nginx

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
