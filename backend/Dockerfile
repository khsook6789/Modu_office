# ─────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────
FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app

# 의존성 레이어 캐시 (소스 변경 시 재다운로드 방지)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon --console=plain 2>/dev/null || true

# 소스 복사 및 빌드
COPY src ./src
RUN gradle build -x test --no-daemon --console=plain

# ─────────────────────────────────────────
# Stage 2: Run
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# 헬스체크용 wget 설치 및 비root 사용자 생성 (보안)
RUN apk add --no-cache wget && \
    addgroup -S spring && adduser -S spring -G spring

USER spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
