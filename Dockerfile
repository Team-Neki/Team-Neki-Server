# Layer extraction stage
FROM eclipse-temurin:21-jre-alpine AS builder

WORKDIR /app

# GitHub Actions에서 빌드된 JAR 파일 복사
COPY build/libs/*.jar app.jar

# Spring Boot Layered JAR에서 레이어 추출
RUN java -Djarmode=layertools -jar app.jar extract

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 보안을 위해 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 레이어별로 복사 (변경 빈도가 낮은 순서대로)
# 1. dependencies: 외부 라이브러리 (거의 변경 안 됨)
COPY --from=builder --chown=spring:spring /app/dependencies/ ./

# 2. spring-boot-loader: Spring Boot 로더
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./

# 3. snapshot-dependencies: SNAPSHOT 의존성
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./

# 4. application: 애플리케이션 코드 (자주 변경됨)
COPY --from=builder --chown=spring:spring /app/application/ ./

# non-root 사용자로 전환
USER spring:spring

# 환경변수 설정 (기본값, 런타임에 오버라이드 가능)
ENV SPRING_PROFILES_ACTIVE=staging
ENV JASYPT_PASSWORD=""

# 애플리케이션 실행
# Layered JAR는 org.springframework.boot.loader.launch.JarLauncher를 사용
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]