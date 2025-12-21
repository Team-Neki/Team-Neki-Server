variable "bucket_name" {
  description = "Name of the S3 bucket"
  type        = string
}

variable "environment" {
  description = "Environment name (e.g., staging, prod)"
  type        = string
}

variable "tags" {
  description = "Additional tags for the S3 bucket"
  type        = map(string)
  default     = {}
}

variable "preset" {
  description = <<-EOT
    Preset configuration for S3 bucket. Available options:
    - public: Public bucket with public-read ACL, no versioning
    - private: Private bucket with versioning enabled
    - archive: Private bucket with versioning and lifecycle rules for archival
  EOT
  type        = string
  default     = "public"
  validation {
    condition     = contains(["public", "private", "archive"], var.preset)
    error_message = "preset must be one of: public, private, archive"
  }
}

# Public Access Configuration (Override preset values if specified)
variable "block_public_acls" {
  description = "Whether Amazon S3 should block public ACLs for this bucket. If null, uses preset value"
  type        = bool
  default     = null
}

variable "block_public_policy" {
  description = "Whether Amazon S3 should block public bucket policies for this bucket. If null, uses preset value"
  type        = bool
  default     = null
}

variable "ignore_public_acls" {
  description = "Whether Amazon S3 should ignore public ACLs for this bucket. If null, uses preset value"
  type        = bool
  default     = null
}

variable "restrict_public_buckets" {
  description = "Whether Amazon S3 should restrict public bucket policies for this bucket. If null, uses preset value"
  type        = bool
  default     = null
}

# Bucket Ownership and ACL
variable "object_ownership" {
  description = "Object ownership setting for the bucket"
  type        = string
  default     = "BucketOwnerPreferred"
  validation {
    condition     = contains(["BucketOwnerPreferred", "ObjectWriter", "BucketOwnerEnforced"], var.object_ownership)
    error_message = "object_ownership must be one of: BucketOwnerPreferred, ObjectWriter, BucketOwnerEnforced"
  }
}

variable "acl" {
  description = "Canned ACL to apply to the bucket. If null, uses preset value"
  type        = string
  default     = null
}

# Versioning
variable "enable_versioning" {
  description = "Enable versioning for the S3 bucket. If null, uses preset value"
  type        = bool
  default     = null
}

# CORS Configuration
variable "enable_cors" {
  description = "Enable CORS configuration for the S3 bucket"
  type        = bool
  default     = true
}

variable "cors_rules" {
  description = "List of CORS rules"
  type = list(object({
    allowed_headers = list(string)
    allowed_methods = list(string)
    allowed_origins = list(string)
    expose_headers  = optional(list(string))
    max_age_seconds = optional(number)
  }))
  default = [
    {
      allowed_headers = ["*"]
      allowed_methods = ["GET", "HEAD"]
      allowed_origins = ["*"] # Update with specific on-premise server domain/IP
      expose_headers  = ["ETag"]
      max_age_seconds = 3000
    }
  ]
}

# Lifecycle Configuration
variable "enable_lifecycle" {
  description = "Enable lifecycle configuration for the S3 bucket. If null, uses preset value"
  type        = bool
  default     = null
}

variable "lifecycle_rules" {
  description = "List of lifecycle rules"
  type = list(object({
    id     = string
    status = string
    filter = optional(object({
      prefix = optional(string)
      tags   = optional(map(string))
    }))
    transitions = optional(list(object({
      days          = number
      storage_class = string
    })))
    expiration = optional(object({
      days = number
    }))
    noncurrent_version_transitions = optional(list(object({
      noncurrent_days = number
      storage_class   = string
    })))
    noncurrent_version_expiration = optional(object({
      noncurrent_days = number
    }))
  }))
  default = []
}

# Bucket Policy
variable "bucket_policy" {
  description = "JSON policy document for the bucket. Set to null to disable"
  type        = string
  default     = null
}

# Encryption
variable "sse_algorithm" {
  description = "Server-side encryption algorithm to use"
  type        = string
  default     = "AES256"
  validation {
    condition     = contains(["AES256", "aws:kms"], var.sse_algorithm)
    error_message = "sse_algorithm must be either AES256 or aws:kms"
  }
}

variable "bucket_key_enabled" {
  description = "Whether to enable S3 Bucket Key for SSE-KMS"
  type        = bool
  default     = false
}

# On-premise Server Access (for future use)
variable "allowed_ips" {
  description = "List of on-premise server IPs allowed to access the bucket. Used for bucket policy generation"
  type        = list(string)
  default     = []
}

variable "allowed_domains" {
  description = "List of on-premise server domains allowed to access the bucket. Used for CORS configuration"
  type        = list(string)
  default     = []
}
