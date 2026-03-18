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

# 비root 사용자 실행 (보안)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
