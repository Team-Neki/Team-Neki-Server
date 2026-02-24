# S3 모듈

퍼블릭/프라이빗 설정을 지원하는 AWS S3 버킷을 생성·관리하기 위한 Terraform 모듈입니다.

## 주요 기능

- ✅ **Preset 기반 간편 설정**: `preset` 변수 하나로 public/private/archive 구성 자동화
- ✅ 퍼블릭/프라이빗 접근 제어
- ✅ 온프레미스 서버 접근을 위한 CORS 설정
- ✅ 버저닝 지원
- ✅ 비용 최적화를 위한 Lifecycle 규칙
- ✅ 서버 사이드 암호화 (AES256 또는 KMS)
- ✅ IP 기반 접근 제어를 위한 Bucket Policy 지원
- ✅ 멀티 환경(staging, prod) 지원
- ✅ Deprecated 속성 미사용 (AWS Provider 5.x)

## Preset 종류

| Preset    | 용도                 | ACL         | Public Block | Versioning | Lifecycle     |
|-----------|--------------------|-------------|--------------|------------|---------------|
| `public`  | 퍼블릭 리소스 저장 (이미지 등) | public-read | 비활성화         | 비활성화       | 비활성화          |
| `private` | 프라이빗 리소스 저장        | private     | 활성화          | 활성화        | 비활성화          |
| `archive` | 장기 보관용 아카이브        | private     | 활성화          | 활성화        | 활성화 (자동 아카이빙) |

## 사용 방법

### 1. Public Bucket (기본 설정)

가장 간단한 사용 예제입니다. 이미지 등 퍼블릭 리소스 저장용입니다.

```hcl
module "public_images" {
  source = "../../modules/s3"

  bucket_name = "yapp-public-images-staging"
  environment = "staging"
  preset      = "public"  # 기본값이므로 생략 가능

  # CORS 설정 (선택사항)
  cors_rules = [
    {
      allowed_headers = ["*"]
      allowed_methods = ["GET", "HEAD", "PUT", "POST"]
      allowed_origins = ["https://your-domain.com"]  # 실제 도메인으로 변경
      expose_headers = ["ETag"]
      max_age_seconds = 3000
    }
  ]

  tags = {
    Project = "Neki"
    Purpose = "Public Image Storage"
  }
}
```

### 2. Private Bucket

프라이빗 리소스 저장용입니다. 버저닝이 자동으로 활성화됩니다.

```hcl
module "private_files" {
  source = "../../modules/s3"

  bucket_name = "yapp-private-files-staging"
  environment = "staging"
  preset = "private"

  # 온프레미스 서버 IP 기반 접근 제어 (선택사항)
  bucket_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowOnPremiseServerAccess"
        Effect    = "Allow"
        Principal = "*"
        Action = ["s3:GetObject", "s3:PutObject"]
        Resource  = "arn:aws:s3:::yapp-private-files-staging/*"
        Condition = {
          IpAddress = {
            "aws:SourceIp" = ["xxx.xxx.xxx.xxx/32"]  # 온프레미스 서버 IP
          }
        }
      }
    ]
  })

  tags = {
    Project = "Neki"
    Purpose = "Private File Storage"
  }
}
```

### 3. Archive Bucket

장기 보관용 아카이브입니다. 버저닝과 Lifecycle 규칙이 자동으로 설정됩니다.

```hcl
module "archive_storage" {
  source = "../../modules/s3"

  bucket_name = "yapp-archive-staging"
  environment = "staging"
  preset      = "archive"

  tags = {
    Project = "Neki"
    Purpose = "Long-term Archive Storage"
  }
}
```

**Archive Preset의 기본 Lifecycle 규칙:**

- 이전 버전: 30일 후 STANDARD_IA, 90일 후 GLACIER로 이동, 180일 후 삭제
- 현재 객체: 90일 후 GLACIER로 이동

### 4. 커스텀 설정 (Preset Override)

Preset을 사용하면서 일부 설정만 변경할 수 있습니다.

```hcl
module "custom_public" {
  source = "../../modules/s3"

  bucket_name = "yapp-custom-staging"
  environment = "staging"
  preset = "public"

  # Preset 값을 override
  enable_versioning = true  # public preset은 기본적으로 versioning이 꺼져있지만, 활성화

  # 커스텀 Lifecycle 규칙 추가
  enable_lifecycle = true
  lifecycle_rules = [
    {
      id     = "delete-old-files"
      status = "Enabled"
      expiration = {
        days = 90
      }
    }
  ]

  tags = {
    Project = "Neki"
    Purpose = "Custom Configuration"
  }
}
```

## 마이그레이션: Public → Private

운영 중인 퍼블릭 버킷을 프라이빗으로 전환하는 방법:

```hcl
module "public_images" {
  source = "../../modules/s3"

  bucket_name = "yapp-public-images-staging"
  environment = "staging"

  # 1. preset만 변경
  preset = "private"  # "public" → "private"로 변경

  # 2. 필요시 bucket policy 추가
  bucket_policy = jsonencode({
    # IP 기반 접근 제어 정책
  })

  tags = {
    Project = "Neki"
    Purpose = "Now Private Image Storage"
  }
}
```

**변경 사항 적용:**

```bash
terraform plan   # 변경 내용 확인
terraform apply  # 적용
```

## Variables

### 필수 변수

| Name          | Description           | Type   |
|---------------|-----------------------|--------|
| `bucket_name` | S3 버킷명                | string |
| `environment` | 환경 이름 (staging, prod) | string |

### 주요 선택 변수

| Name              | Description                              | Type         | Default    |
|-------------------|------------------------------------------|--------------|------------|
| `preset`          | 사전 정의된 설정 (public, private, archive)     | string       | "public"   |
| `cors_rules`      | CORS 규칙 목록                               | list(object) | 기본 CORS 규칙 |
| `bucket_policy`   | JSON 형식 버킷 정책                            | string       | null       |
| `lifecycle_rules` | 커스텀 Lifecycle 규칙 (archive preset은 자동 생성) | list(object) | []         |
| `tags`            | 추가 태그                                    | map(string)  | {}         |

### Override 변수 (Preset 값 재정의)

Preset을 사용하면서 특정 값만 변경하고 싶을 때 사용합니다.

| Name                      | Description   | Type   | Default            |
|---------------------------|---------------|--------|--------------------|
| `acl`                     | Canned ACL    | string | null (preset 값 사용) |
| `block_public_acls`       | 퍼블릭 ACL 차단    | bool   | null (preset 값 사용) |
| `block_public_policy`     | 퍼블릭 정책 차단     | bool   | null (preset 값 사용) |
| `ignore_public_acls`      | 퍼블릭 ACL 무시    | bool   | null (preset 값 사용) |
| `restrict_public_buckets` | 퍼블릭 버킷 제한     | bool   | null (preset 값 사용) |
| `enable_versioning`       | 버저닝 활성화       | bool   | null (preset 값 사용) |
| `enable_lifecycle`        | Lifecycle 활성화 | bool   | null (preset 값 사용) |

## Outputs

| Name                          | Description            |
|-------------------------------|------------------------|
| `bucket_id`                   | 버킷 이름                  |
| `bucket_arn`                  | 버킷 ARN                 |
| `bucket_domain_name`          | 버킷 도메인명                |
| `bucket_regional_domain_name` | 리전별 버킷 도메인             |
| `bucket_region`               | 버킷이 위치한 AWS 리전         |
| `bucket_hosted_zone_id`       | Route53 Hosted Zone ID |

## 사용 팁

### 1. 온프레미스 서버 접근 설정

**CORS 설정 (브라우저 접근)**

```hcl
cors_rules = [
  {
    allowed_origins = ["https://your-domain.com", "http://xxx.xxx.xxx.xxx"]
    allowed_methods = ["GET", "HEAD", "PUT", "POST"]
    # ...
  }
]
```

**Bucket Policy (서버 접근)**

```hcl
bucket_policy = jsonencode({
  Statement = [
    {
      Condition = {
        IpAddress = {
          "aws:SourceIp" = ["xxx.xxx.xxx.xxx/32"]
        }
      }
      # ...
    }
  ]
})
```

### 2. 환경별 설정

**Staging: 비용 절감 우선**

```hcl
preset            = "public"
enable_versioning = false
```

**Production: 데이터 보호 우선**

```hcl
preset            = "private"
enable_versioning = true  # preset이 private이면 기본 활성화
```

### 3. Workspace 활용

```bash
# Staging
terraform workspace select staging
terraform apply -var-file="staging.tfvars"

# Production
terraform workspace select prod
terraform apply -var-file="prod.tfvars"
```

## 요구사항

- Terraform >= 1.0
- AWS Provider >= 5.0

## 예제 프로젝트 구조

```
infra/terraform/aws/
├── modules/
│   └── s3/              # 이 모듈
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── README.md
├── staging/
│   ├── provider.tf
│   ├── main.tf          # 모듈 사용
│   ├── variables.tf
│   ├── outputs.tf
│   └── .terraform.lock.hcl
└── prod/
    ├── provider.tf
    ├── main.tf
    ├── variables.tf
    └── outputs.tf
```
