# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle wrapper와 설정 파일 복사 (캐싱 최적화를 위해 먼저 복사)
COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (소스 코드 변경 시에도 이 레이어는 캐시됨)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안을 위해 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 환경변수 설정 (기본값, 런타임에 오버라이드 가능)
ENV SPRING_PROFILES_ACTIVE=staging
ENV JASYPT_PASSWORD=""

# 애플리케이션 실행
# Spring Boot가 SPRING_PROFILES_ACTIVE, JASYPT_PASSWORD 환경변수를 자동으로 읽음
ENTRYPOINT ["java", "-jar", "app.jar"]