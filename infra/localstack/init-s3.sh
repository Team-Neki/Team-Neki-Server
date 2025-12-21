#!/bin/bash

# LocalStack S3 버킷 초기화 스크립트
echo "Initializing S3 buckets..."

# yapp-local 버킷 생성
awslocal s3 mb s3://yapp-local 2>/dev/null || echo "Bucket yapp-local already exists"

# CORS 설정 적용
echo "Applying CORS configuration to yapp-local bucket..."
awslocal s3api put-bucket-cors --bucket yapp-local --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:63342",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:63342"
      ],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "x-amz-server-side-encryption", "x-amz-request-id", "x-amz-id-2"],
      "MaxAgeSeconds": 3600
    }
  ]
}'

# 버킷 목록 확인
echo "Current S3 buckets:"
awslocal s3 ls

# CORS 설정 확인
echo "CORS configuration for yapp-local:"
awslocal s3api get-bucket-cors --bucket yapp-local

echo "S3 initialization completed"
