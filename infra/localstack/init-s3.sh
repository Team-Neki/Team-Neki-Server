#!/bin/bash

# ========================================
# LocalStack S3 버킷 초기화 스크립트
# ========================================

echo "Initializing S3 buckets..."

# yapp-local 버킷 생성
awslocal s3 mb s3://yapp-local 2>/dev/null || echo "Bucket yapp-local already exists"

# 버킷 목록 확인
echo "Current S3 buckets:"
awslocal s3 ls

echo ""
echo "✅ S3 bucket initialization completed"
echo "ℹ️  CORS configuration will be applied automatically by S3BucketInitializer on application startup"
