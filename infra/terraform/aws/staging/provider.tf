terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configuration (optional - uncomment when ready)
  # backend "s3" {
  #   bucket         = "yapp-terraform-state-staging"
  #   key            = "staging/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   encrypt        = true
  #   dynamodb_table = "yapp-terraform-locks"
  # }
}

provider "aws" {
  region = var.aws_region
  profile = "yapp"

  default_tags {
    tags = {
      Project     = "YAPP"
      Environment = "staging"
      ManagedBy   = "Terraform"
    }
  }
}
