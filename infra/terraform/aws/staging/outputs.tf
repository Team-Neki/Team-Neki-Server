output "public_images_bucket_id" {
  description = "Public images bucket ID"
  value       = module.public_images.bucket_id
}

output "public_images_bucket_arn" {
  description = "Public images bucket ARN"
  value       = module.public_images.bucket_arn
}

output "public_images_bucket_domain_name" {
  description = "Public images bucket domain name"
  value       = module.public_images.bucket_domain_name
}

output "public_images_bucket_url" {
  description = "Public images bucket URL"
  value       = "https://${module.public_images.bucket_domain_name}"
}

output "public_images_bucket_regional_domain_name" {
  description = "Public images bucket regional domain name"
  value       = module.public_images.bucket_regional_domain_name
}
