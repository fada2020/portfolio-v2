# 멀티스테이지 빌드로 이미지 크기 최적화

# Stage 1: Build
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Gradle wrapper와 의존성 파일만 먼저 복사 (캐시 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드 (테스트 스킵)
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 애플리케이션 사용자 생성 (보안)
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유권 변경
RUN chown appuser:appuser app.jar

# 애플리케이션 사용자로 전환
USER appuser

# 헬스체크 (Docker 컨테이너 상태 확인)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# JVM 메모리 옵션 (프리티어 t2.micro: 1GB RAM 고려)
# -Xmx: 최대 힙 메모리 512MB (시스템 메모리의 약 50%)
# -Xms: 초기 힙 메모리 256MB
# -XX:MaxMetaspaceSize: 메타스페이스 최대 128MB
# -XX:+UseContainerSupport: 컨테이너 환경 인식
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseContainerSupport -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
