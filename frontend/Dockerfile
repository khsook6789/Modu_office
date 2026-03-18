# ─────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────
FROM node:22-alpine AS builder
WORKDIR /app

# 빌드 시 주입할 Vite 환경변수
ARG VITE_API_BASE_URL=/api
ARG VITE_GOOGLE_MAPS_API_KEY
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_GOOGLE_MAPS_API_KEY=$VITE_GOOGLE_MAPS_API_KEY

# 의존성 레이어 캐시 (package.json 변경 시에만 재설치)
COPY package.json package-lock.json ./
RUN npm ci

# 소스 복사 및 빌드
COPY . .
RUN npm run build

# ─────────────────────────────────────────
# Stage 2: Serve
# ─────────────────────────────────────────
FROM nginx:alpine AS runner

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
