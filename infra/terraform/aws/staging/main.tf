# Public Images Bucket
module "object_storage" {
  source = "../modules/s3"

  bucket_name = var.bucket_name
  environment = "staging"
  preset      = "private" # public/private/archive 중 선택

  # on-premise & browser 접근용 CORS 설정
  cors_rules = [
    {
      allowed_headers = ["*"]
      allowed_methods = ["GET", "HEAD", "PUT", "POST"]
      allowed_origins = var.allowed_origins
      expose_headers  = ["ETag"]
      max_age_seconds = 3000
    }
  ]

  # Bucket Policy: 읽기는 누구나, 업로드는 특정 도메인에서만
  bucket_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowBackendAccess"
        Effect    = "Allow"
        Principal = "*"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = "arn:aws:s3:::${var.bucket_name}/*"
        Condition = {
          StringLike = {
            "aws:SourceIp" = var.allowed_server_ips
          }
        }
      }
    ]
  })

  tags = {
    Project = "Neki"
    Purpose = "Public Image Storage"
  }
}
