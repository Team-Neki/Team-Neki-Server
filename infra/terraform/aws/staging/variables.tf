variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "public_images_bucket_name" {
  description = "Name of the public images S3 bucket"
  type        = string
  default     = "yapp-public-images-staging"
}

variable "allowed_origins" {
  description = "List of allowed origins for CORS (browser access)"
  type        = list(string)
  default     = ["*"] # Update with specific frontend domains
  # Example: ["https://your-frontend.com", "http://localhost:3000"]
}

variable "allowed_server_ips" {
  description = "List of allowed IP addresses for upload operations"
  type        = list(string)
  default     = ["*"] # Update with specific on-premise server IPs
  # Example: ["203.0.113.0/24", "198.51.100.5"]
}
