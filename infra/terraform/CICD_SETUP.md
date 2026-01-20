# Terraform CI/CD Setup Guide

이 문서는 Terraform CI/CD 파이프라인 설정 방법을 설명합니다.

## 📋 목차

1. [파이프라인 개요](#파이프라인-개요)
2. [사전 요구사항](#사전-요구사항)
3. [AWS 설정](#aws-설정)
4. [GitHub Secrets 설정](#github-secrets-설정)
5. [Discord Webhook 설정](#discord-webhook-설정)
6. [Terraform Backend 설정](#terraform-backend-설정)
7. [파이프라인 동작 방식](#파이프라인-동작-방식)
8. [트러블슈팅](#트러블슈팅)

---

## 파이프라인 개요

### 워크플로우 구조

```
PR 생성/업데이트
  ↓
Terraform Plan 실행
  ↓
PR에 Plan 결과 코멘트

Main 브랜치 머지
  ↓
Terraform Apply 실행
  ↓
성공: Discord 성공 알림
실패: Discord 실패 알림
```

### 주요 기능

- ✅ PR에서 자동으로 `terraform plan` 실행 및 결과 코멘트
- ✅ Main 브랜치 머지 시 자동으로 `terraform apply` 실행
- ✅ CD 실패/성공 시 Discord 알림
- ✅ Terraform 포맷 체크, Validate 검증
- ✅ AWS Credentials 안전한 관리

---

## 사전 요구사항

- AWS 계정 및 IAM 사용자
- GitHub repository 접근 권한
- Discord 서버 관리자 권한

---

## AWS 설정

### 1. IAM 사용자 생성

GitHub Actions에서 사용할 IAM 사용자를 생성합니다.

```bash
# AWS Console에서 IAM 사용자 생성
# 이름: github-actions-terraform (또는 원하는 이름)
```

### 2. IAM 정책 연결

최소 권한 원칙에 따라 필요한 권한만 부여합니다.

**현재 프로젝트 기준 필요한 권한:**
- S3 관리 권한 (버킷 생성, 수정, 삭제, 정책 관리)
- S3 Backend 접근 권한 (tfstate 저장용)
- DynamoDB 접근 권한 (state lock용)

**정책 예시 (`terraform-ci-cd-policy.json`):**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:*"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:DeleteItem",
        "dynamodb:DescribeTable"
      ],
      "Resource": "arn:aws:dynamodb:ap-northeast-2:*:table/yapp-terraform-locks"
    }
  ]
}
```

> ⚠️ **보안 권장사항**: 프로덕션 환경에서는 더 세밀한 권한 제어가 필요합니다.

### 3. Access Key 생성

```bash
# AWS Console → IAM → Users → [사용자] → Security credentials → Create access key
# Access Key Type: Command Line Interface (CLI)
```

생성된 Access Key ID와 Secret Access Key를 안전하게 보관합니다.

---

## GitHub Secrets 설정

### 1. Repository Secrets 추가

GitHub repository → Settings → Secrets and variables → Actions → New repository secret

### 2. 필요한 Secrets

| Secret Name | 설명 | 예시 |
|------------|------|------|
| `AWS_ACCESS_KEY_ID` | AWS IAM 사용자 Access Key ID | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM 사용자 Secret Access Key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `DISCORD_WEBHOOK_URL` | Discord Webhook URL | `https://discord.com/api/webhooks/...` |

### 3. Secrets 설정 방법

```bash
# GitHub CLI 사용 시
gh secret set AWS_ACCESS_KEY_ID -b "YOUR_ACCESS_KEY_ID"
gh secret set AWS_SECRET_ACCESS_KEY -b "YOUR_SECRET_ACCESS_KEY"
gh secret set DISCORD_WEBHOOK_URL -b "YOUR_DISCORD_WEBHOOK_URL"
```

또는 GitHub 웹 UI에서 직접 입력합니다.

---

## Discord Webhook 설정

### 1. Discord 서버 Webhook 생성

1. Discord 서버 설정 → 연동 → Webhooks
2. "새 Webhook" 클릭
3. Webhook 이름 설정 (예: `Terraform Bot`)
4. 알림을 받을 채널 선택
5. "Webhook URL 복사"

### 2. Webhook URL 형식

```
https://discord.com/api/webhooks/{webhook.id}/{webhook.token}
```

### 3. 테스트

```bash
curl -X POST "$DISCORD_WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Terraform CI/CD 설정 테스트 메시지입니다."
  }'
```

---

## Terraform Backend 설정

현재 Backend가 주석 처리되어 있어 로컬에 state가 저장됩니다.
프로덕션 환경에서는 S3 Backend 사용을 권장합니다.

### 1. Backend용 S3 버킷 생성

```bash
# AWS CLI 사용
aws s3 mb s3://yapp-terraform-state-staging --region ap-northeast-2

# 버킷 버전 관리 활성화
aws s3api put-bucket-versioning \
  --bucket yapp-terraform-state-staging \
  --versioning-configuration Status=Enabled

# 버킷 암호화 활성화
aws s3api put-bucket-encryption \
  --bucket yapp-terraform-state-staging \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

### 2. DynamoDB 테이블 생성 (State Lock용)

```bash
aws dynamodb create-table \
  --table-name yapp-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region ap-northeast-2
```

### 3. Provider.tf에서 Backend 활성화

`infra/terraform/aws/staging/provider.tf` 파일에서 주석 해제:

```terraform
terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "yapp-terraform-state-staging"
    key            = "staging/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "yapp-terraform-locks"
  }
}
```

### 4. Backend 초기화

```bash
cd infra/terraform/aws/staging
terraform init -migrate-state
```

---

## 파이프라인 동작 방식

### PR 워크플로우

1. `infra/terraform/**` 경로에 변경사항이 있는 PR 생성
2. GitHub Actions가 자동으로 실행:
   - Terraform Format 체크
   - Terraform Init
   - Terraform Validate
   - Terraform Plan
3. Plan 결과가 PR에 코멘트로 추가됨
4. 리뷰어가 Plan 결과 확인 후 승인

### Deploy 워크플로우

1. PR이 Main 브랜치에 머지됨
2. GitHub Actions가 자동으로 실행:
   - Terraform Init
   - Terraform Apply (자동 승인)
3. 결과에 따라 Discord 알림:
   - ✅ 성공: 녹색 알림
   - 🚨 실패: 빨간색 알림 + 에러 정보

---

## 트러블슈팅

### 1. AWS Credentials 에러

**증상:**
```
Error: error configuring Terraform AWS Provider: no valid credential sources
```

**해결:**
- GitHub Secrets에 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`가 올바르게 설정되었는지 확인
- IAM 사용자에게 필요한 권한이 있는지 확인

### 2. Terraform State Lock 에러

**증상:**
```
Error: Error acquiring the state lock
```

**해결:**
```bash
# DynamoDB에서 Lock 확인
aws dynamodb scan --table-name yapp-terraform-locks

# Lock 해제 (주의: 실제로 다른 작업이 진행 중인지 확인 후 실행)
terraform force-unlock <LOCK_ID>
```

### 3. Discord Webhook 에러

**증상:**
- Discord 알림이 오지 않음

**해결:**
- `DISCORD_WEBHOOK_URL`이 올바르게 설정되었는지 확인
- Discord 채널에서 Webhook이 삭제되지 않았는지 확인
- curl로 직접 테스트

### 4. Terraform Format 실패

**증상:**
```
Terraform Format Check failed
```

**해결:**
```bash
# 로컬에서 포맷 적용
cd infra/terraform
terraform fmt -recursive

# 커밋 후 다시 푸시
git add .
git commit -m "chore: apply terraform format"
git push
```

### 5. 권한 부족 에러

**증상:**
```
Error: AccessDenied: User is not authorized to perform
```

**해결:**
- IAM 정책 확인 및 필요한 권한 추가
- 정책 변경 후 약간의 시간(~1분) 대기

---

## 추가 개선 사항 (선택)

현재는 MVP 단계이므로 최소 구성이지만, 향후 개선 가능한 사항:

1. **환경별 워크플로우 분리**
   - Staging, Production 환경 별도 관리
   - 수동 승인 단계 추가 (Production)

2. **Terraform Plan 아티팩트 저장**
   - Plan 결과를 아티팩트로 저장
   - Apply 시 저장된 Plan 사용

3. **Drift Detection**
   - 정기적으로 Terraform State와 실제 인프라 비교
   - 차이 발견 시 알림

4. **Cost Estimation**
   - Infracost 통합
   - PR에 비용 변화 코멘트

5. **Security Scanning**
   - tfsec, Checkov 등 보안 스캔 도구 추가

---

## 참고 자료

- [Terraform GitHub Actions](https://github.com/hashicorp/setup-terraform)
- [AWS Actions](https://github.com/aws-actions/configure-aws-credentials)
- [Discord Webhook Guide](https://discord.com/developers/docs/resources/webhook)
- [Terraform Backend Configuration](https://www.terraform.io/language/settings/backends/s3)
