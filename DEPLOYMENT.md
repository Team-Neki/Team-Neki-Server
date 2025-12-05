# Deployment Guide

## 개요

이 프로젝트는 GitHub Actions를 사용하여 온프레미스 서버에 **블루-그린(Blue-Green) 배포 방식**으로 Docker 컨테이너를 자동 배포합니다.

## 배포 전략: 블루-그린 배포

### 블루-그린 배포란?

두 개의 동일한 프로덕션 환경(Blue, Green)을 유지하며, 하나는 활성(Active), 다른 하나는 대기(Standby) 상태로 운영하는 배포 방식입니다.

### 장점

- **무중단 배포**: 새 버전을 배포해도 서비스 중단 없음
- **즉시 롤백**: 문제 발생시 이전 버전으로 즉시 전환 가능
- **안전한 배포**: 새 버전이 정상 동작 확인 후 트래픽 전환
- **백업 불필요**: Git revert로 이전 버전 복구 가능

### 동작 방식 (Nginx + 내부 포트 2개)

```
배포 전:
Nginx (8080) → Blue 컨테이너 (내부 포트 9080)

1. Green 컨테이너를 내부 포트 9081로 시작 (새 버전)
2. Green 컨테이너 헬스체크 (60초 동안 30번 시도)
3. 헬스체크 성공 → Nginx upstream을 9081로 변경
4. Nginx reload (무중단)
5. Blue 컨테이너 중지

배포 후:
Nginx (8080) → Green 컨테이너 (내부 포트 9081)

다음 배포: Green 활성 상태이므로 Blue(9080)로 배포
```

**포트 충돌 해결**: Blue와 Green이 각각 다른 내부 포트를 사용하므로 동시에 실행 가능하며, Nginx가 헬스체크 성공한 컨테이너로만 트래픽을 라우팅합니다.

## 환경 구성

### 배포 환경

- **Production (상용)**: `main` 브랜치
  - Nginx 외부 포트: `443`
  - Domain: `yapp.suitestudy.com`
  - Blue 컨테이너: `yapp-app-prod-blue` (내부 포트 `9080`)
  - Green 컨테이너: `yapp-app-prod-green` (내부 포트 `9081`)

- **Staging (개발)**: `staging` 브랜치
  - Nginx 외부 포트: `443`
  - Domain: `dev-yapp.suitestudy.com`
  - Blue 컨테이너: `yapp-app-staging-blue` (내부 포트 `9082`)
  - Green 컨테이너: `yapp-app-staging-green` (내부 포트 `9083`)

두 환경 모두 동일한 온프레미스 서버에 배포되며, Nginx 포트로 구분됩니다.

## GitHub Secrets 설정

GitHub 저장소의 Settings → Secrets and variables → Actions에서 다음 secrets를 설정해야 합니다:

### Production 환경

- `PROD_SERVER_HOST`: 상용 서버 호스트 (IP 또는 도메인)
- `PROD_SERVER_USER`: SSH 접속 사용자명
- `PROD_SERVER_SSH_KEY`: SSH private key (전체 내용)
- `PROD_SERVER_SSH_PORT`: SSH 포트 (기본값: 22)

### Staging 환경

- `STAGING_SERVER_HOST`: 개발 서버 호스트 (상용과 동일한 서버)
- `STAGING_SERVER_USER`: SSH 접속 사용자명 (상용과 동일)
- `STAGING_SERVER_SSH_KEY`: SSH private key (상용과 동일)
- `STAGING_SERVER_SSH_PORT`: SSH 포트 (상용과 동일)

> **참고**: Production과 Staging이 같은 온프레미스 서버를 사용하는 경우, HOST/USER/SSH_KEY/SSH_PORT 값이 동일합니다.

## 서버 사전 준비사항

온프레미스 서버에 다음이 설치되어 있어야 합니다:

```bash
# Docker 설치 확인
docker --version

# Nginx 설치 확인
nginx -v

# 필요한 포트 확인
# - Nginx: 8080 (Production), 8081 (Staging)
# - Docker 내부: 9080, 9081, 9082, 9083
sudo netstat -tulpn | grep -E '8080|8081|9080|9081|9082|9083'

# SSH 키 기반 인증 설정 (비밀번호 없이 접속 가능해야 함)
# GitHub Actions에서 사용할 SSH public key를 서버의 authorized_keys에 추가
```

### Nginx 설정 (Docker)

**주의**: Nginx가 Docker 컨테이너로 실행되고 있으므로, 설정 파일을 호스트에서 마운트합니다.

#### 1. docker-compose.yml 업데이트

기존 `~/project/nginx/docker-compose.yml`에 upstream 설정 파일 마운트 추가:

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:latest
    container_name: darren-nginx
    ports:
      - "80:80"
      - "443:443"
      - "8080:8080"    # Production 포트 추가
      - "8081:8081"    # Staging 포트 추가
    volumes:
      - ./default.conf:/etc/nginx/conf.d/default.conf
      - ./production.conf:/etc/nginx/conf.d/production.conf           # Production 설정
      - ./staging.conf:/etc/nginx/conf.d/staging.conf                 # Staging 설정
      - ./production-upstream.conf:/etc/nginx/conf.d/production-upstream.conf  # Upstream (동적)
      - ./staging-upstream.conf:/etc/nginx/conf.d/staging-upstream.conf       # Upstream (동적)
      - /etc/letsencrypt:/etc/letsencrypt
      - /var/lib/letsencrypt:/var/lib/letsencrypt
      - ./certbot-www:/var/www/certbot
      - ./static:/usr/share/nginx/html
      - /var/www/html:/var/www/html
    extra_hosts:
      - "host.docker.internal:host-gateway"  # Docker에서 호스트 접근
    restart: always
```

#### 2. Production 설정 (`~/project/nginx/production.conf`)

```nginx
# Upstream은 배포 스크립트가 자동으로 업데이트합니다
# /etc/nginx/conf.d/production-upstream.conf

server {
    listen 443 ssl;
    server_name yapp.suitstudy.com;

    ssl_certificate /etc/letsencrypt/live/yapp.suitestudy.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yapp.suitestudy.com/privkey.pem;

    location / {
        proxy_pass http://production_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeout 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check endpoint
    location /actuator/health {
        proxy_pass http://production_backend/actuator/health;
        access_log off;
    }
}
```

#### 3. Staging 설정 (`~/project/nginx/staging.conf`)

```nginx
# Upstream은 배포 스크립트가 자동으로 업데이트합니다
# /etc/nginx/conf.d/staging-upstream.conf

server {
    listen 443 ssl;
    server_name dev-yapp.suitstudy.com;

    ssl_certificate /etc/letsencrypt/live/dev-yapp.suitestudy.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/dev-yapp.suitestudy.com/privkey.pem;

    location / {
        proxy_pass http://staging_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeout 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health check endpoint
    location /actuator/health {
        proxy_pass http://staging_backend/actuator/health;
        access_log off;
    }
}
```

#### 4. 초기 Upstream 설정

첫 배포 전에 `~/project/nginx/` 디렉토리에 초기 upstream 파일을 생성:

```bash
cd ~/project/nginx

# Production upstream (첫 배포는 blue로 시작)
cat > production-upstream.conf <<EOF
upstream production_backend {
    server host.docker.internal:9080;
}
EOF

# Staging upstream (첫 배포는 blue로 시작)
cat > staging-upstream.conf <<EOF
upstream staging_backend {
    server host.docker.internal:9082;
}
EOF
```

#### 5. Nginx 재시작

```bash
cd ~/project/nginx
docker-compose down
docker-compose up -d

# 설정 확인
docker exec darren-nginx nginx -t

# Nginx 상태 확인
docker ps | grep darren-nginx
```

#### 6. 배포 사용자 권한

배포 스크립트가 Nginx 설정 파일을 수정할 수 있도록 권한 확인:

```bash
# ~/project/nginx/ 디렉토리 소유자 확인
ls -la ~/project/nginx/

# 필요시 권한 변경 (배포 사용자가 darren인 경우)
chown darren:darren ~/project/nginx/*.conf
```

### SSH 키 생성 및 설정

```bash
# 1. 로컬에서 SSH 키 생성
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deploy_key

# 2. Public key를 서버에 추가
ssh-copy-id -i ~/.ssh/deploy_key.pub user@server-ip

# 3. Private key 내용을 GitHub Secrets에 등록
cat ~/.ssh/deploy_key
# 출력된 전체 내용을 복사하여 PROD_SERVER_SSH_KEY, STAGING_SERVER_SSH_KEY에 등록
```

## 배포 프로세스

### Production 배포 (main 브랜치)

```bash
# main 브랜치로 push하면 자동 배포
git checkout main
git merge develop
git push origin main
```

배포 순서:
1. 코드 체크아웃
2. JDK 21 설정
3. Gradle 빌드 및 테스트
4. Docker 이미지 빌드
5. 이미지를 tar.gz로 압축
6. 온프레미스 서버로 SCP 전송
7. **블루-그린 배포 실행**:
   - 현재 활성 컨테이너 확인 (blue/green)
   - 비활성 컨테이너로 새 버전 배포
   - 헬스체크 실행 (60초, 30회 시도)
   - 성공시 이전 컨테이너 중지
   - 실패시 새 컨테이너 중지 및 롤백

### Staging 배포 (staging 브랜치)

```bash
# staging 브랜치로 push하면 자동 배포
git checkout staging
git merge feature-branch
git push origin staging
```

배포 프로세스는 Production과 동일하며, 포트만 8081로 다릅니다.

## 배포 확인

### Production 확인

```bash
# 현재 활성 컨테이너 확인
docker ps | grep yapp-app-prod

# 예시 출력:
# yapp-app-prod-green  (활성)
# 또는
# yapp-app-prod-blue   (활성)

# 로그 확인 (활성 컨테이너)
docker logs yapp-app-prod-green  # 또는 blue

# 헬스체크
curl http://server-ip:8080/actuator/health
```

### Staging 확인

```bash
# 현재 활성 컨테이너 확인
docker ps | grep yapp-app-staging

# 로그 확인
docker logs yapp-app-staging-green  # 또는 blue

# 헬스체크
curl http://server-ip:8081/actuator/health
```

## 롤백 전략

블루-그린 배포는 자동 롤백을 지원합니다:

### 1. 자동 롤백 (배포 중 실패)

헬스체크 실패시 자동으로 롤백됩니다:
- 새 컨테이너 중지 및 삭제
- 기존 컨테이너 유지 (서비스 계속 운영)
- 배포 실패 알림

### 2. 수동 롤백 (배포 후 문제 발견)

Git revert를 사용하여 롤백:

```bash
# 1. 문제가 있는 커밋 revert
git revert HEAD

# 2. main(또는 staging) 브랜치로 push
git push origin main

# 3. 자동 배포가 이전 버전으로 재배포
```

### 3. 긴급 롤백 (서버에서 직접)

서버에서 직접 이전 이미지로 전환:

```bash
# 1. 사용 가능한 이미지 확인
docker images yapp-app-2-server | grep prod

# 2. 현재 활성 컨테이너 확인
docker ps | grep yapp-app-prod

# 3. 현재 컨테이너 중지
docker stop yapp-app-prod-green

# 4. 이전 이미지로 새 컨테이너 시작
docker run -d \
  --name yapp-app-prod-blue \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  yapp-app-2-server:prod-<이전-커밋-SHA>
```

## 수동 배포 (로컬에서)

필요시 로컬에서 수동으로 배포할 수 있습니다:

```bash
# 1. Docker 이미지 빌드
docker build -t yapp-app-2-server:local .

# 2. Production 배포 (블루-그린 방식)
# 현재 활성 컨테이너 확인
CURRENT=$(docker ps --format '{{.Names}}' | grep yapp-app-prod | sed 's/yapp-app-prod-//')

# 새 컨테이너 시작 (반대 색상)
if [ "$CURRENT" = "blue" ]; then
  NEW="green"
else
  NEW="blue"
fi

docker run -d \
  --name yapp-app-prod-$NEW \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  yapp-app-2-server:local

# 헬스체크 후 이전 컨테이너 중지
sleep 10
curl -f http://localhost:8080/actuator/health && docker stop yapp-app-prod-$CURRENT
```

## 트러블슈팅

### 헬스체크 실패

Spring Boot Actuator가 활성화되어 있는지 확인:

```bash
# 로그 확인
docker logs yapp-app-prod-green --tail 100

# Actuator 엔드포인트 확인
curl http://localhost:8080/actuator/health

# 컨테이너 내부 확인
docker exec -it yapp-app-prod-green sh
```

이미 설정되어 있음:
- `build.gradle.kts`: `spring-boot-starter-actuator` 의존성 추가됨
- `application.yml`: health 엔드포인트 활성화됨

### 포트 충돌

포트가 이미 사용 중인 경우:

```bash
# 포트 사용 프로세스 확인
sudo lsof -i :8080

# 또는
sudo netstat -tulpn | grep 8080

# 기존 컨테이너 확인 및 정리
docker ps -a | grep yapp-app
docker stop <container-id>
docker rm <container-id>
```

### 블루-그린 컨테이너 충돌

두 컨테이너가 동시에 같은 포트를 사용하려는 경우:

```bash
# 모든 관련 컨테이너 확인
docker ps -a | grep yapp-app-prod

# 문제가 있는 컨테이너 정리
docker stop yapp-app-prod-blue yapp-app-prod-green
docker rm yapp-app-prod-blue yapp-app-prod-green

# 첫 배포 (blue로 시작)
docker run -d \
  --name yapp-app-prod-blue \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  yapp-app-2-server:latest-prod
```

### SSH 연결 실패

```bash
# SSH 연결 테스트
ssh -i ~/.ssh/deploy_key user@server-ip -p 22

# SSH 키 권한 확인 (600이어야 함)
chmod 600 ~/.ssh/deploy_key

# authorized_keys 권한 확인 (서버에서)
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
```

### Docker 이미지 용량 관리

자동으로 최근 3개 이미지만 유지됩니다. 추가 정리가 필요한 경우:

```bash
# 사용하지 않는 이미지 삭제
docker image prune -a

# 특정 이미지만 삭제
docker rmi yapp-app-2-server:old-tag
```

## 모니터링

### 로그 모니터링

```bash
# 실시간 로그 확인
docker logs -f yapp-app-prod-green

# 최근 100줄
docker logs --tail 100 yapp-app-prod-green

# 특정 시간 이후 로그
docker logs --since 10m yapp-app-prod-green

# 두 컨테이너 동시 모니터링 (배포 중)
docker logs -f yapp-app-prod-blue &
docker logs -f yapp-app-prod-green &
```

### 리소스 모니터링

```bash
# 컨테이너 리소스 사용량
docker stats yapp-app-prod-blue yapp-app-prod-green yapp-app-staging-blue yapp-app-staging-green

# 디스크 사용량
docker system df
```

### 배포 상태 확인

```bash
# GitHub Actions 배포 로그 확인
# GitHub Repository → Actions 탭 → 최근 workflow 확인

# 서버에서 배포 히스토리 확인
docker images yapp-app-2-server --format "table {{.Tag}}\t{{.CreatedAt}}\t{{.Size}}"
```

## 블루-그린 배포 모니터링

### 배포 진행 상황 확인

```bash
# 1. 배포 시작 전: Blue만 실행 중
docker ps --format "{{.Names}}\t{{.Status}}" | grep yapp-app-prod
# 출력: yapp-app-prod-blue   Up 2 hours

# 2. 배포 중: Blue와 Green 모두 실행 (잠깐)
docker ps --format "{{.Names}}\t{{.Status}}" | grep yapp-app-prod
# 출력:
# yapp-app-prod-blue    Up 2 hours
# yapp-app-prod-green   Up 10 seconds

# 3. 배포 완료: Green만 실행 중
docker ps --format "{{.Names}}\t{{.Status}}" | grep yapp-app-prod
# 출력: yapp-app-prod-green   Up 1 minute
```

## 보안 권장사항

1. **SSH 키 관리**: Private key는 절대 git에 커밋하지 않기
2. **환경 변수**: 민감한 정보는 환경 변수로 관리
3. **방화벽**: 필요한 포트만 열기 (8080, 8081, SSH)
4. **정기 업데이트**: Docker 및 베이스 이미지 정기 업데이트
5. **로그 관리**: 개인정보가 포함된 로그는 주기적으로 삭제

## 성능 최적화

### Docker 빌드 캐싱

현재 Dockerfile은 멀티 스테이지 빌드와 레이어 캐싱을 사용하여 최적화되어 있습니다:

- Gradle 의존성을 별도 레이어로 캐싱
- 소스 코드 변경시 의존성 다운로드 스킵
- Runtime 이미지는 JRE만 포함하여 크기 최소화

### 메모리 설정

필요시 JVM 메모리 옵션 추가:

```bash
docker run -d \
  --name yapp-app-prod-blue \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  yapp-app-2-server:latest
```

## 블루-그린 배포 FAQ

### Q: 첫 배포시에는 어떻게 되나요?
A: 첫 배포시 활성 컨테이너가 없으므로 자동으로 blue 컨테이너로 배포됩니다.

### Q: 두 컨테이너가 동시에 같은 포트를 사용하지 않나요?
A: 아니요. Blue와 Green은 각각 다른 내부 포트(9080, 9081)를 사용합니다. Nginx가 헬스체크 성공한 컨테이너로만 트래픽을 라우팅하므로 포트 충돌이 없습니다.

### Q: 배포 중 다운타임이 있나요?
A: 전혀 없습니다! Blue와 Green이 동시에 실행되고, Nginx가 무중단으로 upstream을 전환합니다. 완전한 무중단 배포입니다.

### Q: 백업은 어떻게 하나요?
A: Git 히스토리가 백업 역할을 합니다. 문제 발생시 `git revert`로 이전 버전을 재배포하면 됩니다.

### Q: 데이터베이스 마이그레이션은 어떻게 처리하나요?
A: Flyway나 Liquibase 같은 마이그레이션 도구를 사용하고, 하위 호환성을 유지하는 방향으로 스키마를 변경하세요.