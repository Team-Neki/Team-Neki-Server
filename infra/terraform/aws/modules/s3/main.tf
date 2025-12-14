# Preset configurations
locals {
  preset_configs = {
    public = {
      acl                     = "public-read"  # 읽기만 public, 쓰기는 policy로 제어
      block_public_acls       = false
      block_public_policy     = false
      ignore_public_acls      = false
      restrict_public_buckets = false
      enable_versioning       = false
      enable_lifecycle        = false
    }
    private = {
      acl                     = "private"
      block_public_acls       = true
      block_public_policy     = true
      ignore_public_acls      = true
      restrict_public_buckets = true
      enable_versioning       = true
      enable_lifecycle        = false
    }
    archive = {
      acl                     = "private"
      block_public_acls       = true
      block_public_policy     = true
      ignore_public_acls      = true
      restrict_public_buckets = true
      enable_versioning       = true
      enable_lifecycle        = true
    }
  }

  config = local.preset_configs[var.preset]

  # 최종값으로 override 가능
  acl                     = var.acl != null ? var.acl : local.config.acl
  block_public_acls       = var.block_public_acls != null ? var.block_public_acls : local.config.block_public_acls
  block_public_policy     = var.block_public_policy != null ? var.block_public_policy : local.config.block_public_policy
  ignore_public_acls      = var.ignore_public_acls != null ? var.ignore_public_acls : local.config.ignore_public_acls
  restrict_public_buckets = var.restrict_public_buckets != null ? var.restrict_public_buckets : local.config.restrict_public_buckets
  enable_versioning       = var.enable_versioning != null ? var.enable_versioning : local.config.enable_versioning
  enable_lifecycle        = var.enable_lifecycle != null ? var.enable_lifecycle : local.config.enable_lifecycle

  # preset & custom rule 기반 lifecycle rule 설정
  lifecycle_rules = length(var.lifecycle_rules) > 0 ? var.lifecycle_rules : (
    var.preset == "archive" ? [
      {
        id     = "archive-old-versions"
        status = "Enabled"
        noncurrent_version_transitions = [
          {
            noncurrent_days = 30
            storage_class   = "STANDARD_IA"
          },
          {
            noncurrent_days = 90
            storage_class   = "GLACIER"
          }
        ]
        noncurrent_version_expiration = {
          noncurrent_days = 180
        }
        transitions = null
        expiration  = null
        filter      = null
      },
      {
        id                             = "archive-current-objects"
        status                         = "Enabled"
        noncurrent_version_transitions = null
        noncurrent_version_expiration  = null
        expiration                     = null
        filter                         = null
        transitions = [
          {
            days          = 90
            storage_class = "GLACIER"
          }
        ]
      }
    ] : []
  )
}

# S3 Bucket
resource "aws_s3_bucket" "this" {
  bucket = var.bucket_name

  tags = merge(
    var.tags,
    {
      Name        = var.bucket_name
      Environment = var.environment
      ManagedBy   = "Terraform"
      Preset      = var.preset
    }
  )
}

# Public Access Block Configuration
resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = local.block_public_acls
  block_public_policy     = local.block_public_policy
  ignore_public_acls      = local.ignore_public_acls
  restrict_public_buckets = local.restrict_public_buckets
}

# Bucket Ownership Controls
resource "aws_s3_bucket_ownership_controls" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    object_ownership = var.object_ownership
  }
}

# Bucket ACL
resource "aws_s3_bucket_acl" "this" {
  depends_on = [
    aws_s3_bucket_ownership_controls.this,
    aws_s3_bucket_public_access_block.this
  ]

  bucket = aws_s3_bucket.this.id
  acl    = local.acl
}

# Versioning Configuration
resource "aws_s3_bucket_versioning" "this" {
  count = local.enable_versioning ? 1 : 0

  bucket = aws_s3_bucket.this.id

  versioning_configuration {
    status = "Enabled"
  }
}

# CORS Configuration
resource "aws_s3_bucket_cors_configuration" "this" {
  count = var.enable_cors ? 1 : 0

  bucket = aws_s3_bucket.this.id

  dynamic "cors_rule" {
    for_each = var.cors_rules

    content {
      allowed_headers = cors_rule.value.allowed_headers
      allowed_methods = cors_rule.value.allowed_methods
      allowed_origins = cors_rule.value.allowed_origins
      expose_headers  = lookup(cors_rule.value, "expose_headers", null)
      max_age_seconds = lookup(cors_rule.value, "max_age_seconds", null)
    }
  }
}

# Lifecycle Configuration
resource "aws_s3_bucket_lifecycle_configuration" "this" {
  count = local.enable_lifecycle ? 1 : 0

  bucket = aws_s3_bucket.this.id

  dynamic "rule" {
    for_each = local.lifecycle_rules

    content {
      id     = rule.value.id
      status = rule.value.status

      dynamic "filter" {
        for_each = lookup(rule.value, "filter", null) != null ? [rule.value.filter] : []

        content {
          prefix = lookup(filter.value, "prefix", null)

          dynamic "tag" {
            for_each = lookup(filter.value, "tags", {})

            content {
              key   = tag.key
              value = tag.value
            }
          }
        }
      }

      dynamic "transition" {
        for_each = lookup(rule.value, "transitions", [])

        content {
          days          = lookup(transition.value, "days", null)
          storage_class = transition.value.storage_class
        }
      }

      dynamic "expiration" {
        for_each = lookup(rule.value, "expiration", null) != null ? [rule.value.expiration] : []

        content {
          days = lookup(expiration.value, "days", null)
        }
      }

      dynamic "noncurrent_version_transition" {
        for_each = lookup(rule.value, "noncurrent_version_transitions", [])

        content {
          noncurrent_days = noncurrent_version_transition.value.noncurrent_days
          storage_class   = noncurrent_version_transition.value.storage_class
        }
      }

      dynamic "noncurrent_version_expiration" {
        for_each = lookup(rule.value, "noncurrent_version_expiration", null) != null ? [rule.value.noncurrent_version_expiration] : []

        content {
          noncurrent_days = noncurrent_version_expiration.value.noncurrent_days
        }
      }
    }
  }
}

# Bucket Policy
resource "aws_s3_bucket_policy" "this" {
  count = var.bucket_policy != null ? 1 : 0

  bucket = aws_s3_bucket.this.id
  policy = var.bucket_policy

  depends_on = [
    aws_s3_bucket_public_access_block.this
  ]
}

# Server-side encryption configuration
resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = var.sse_algorithm
    }
    bucket_key_enabled = var.bucket_key_enabled
  }
}
