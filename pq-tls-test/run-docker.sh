#!/bin/bash

# Script to build and run the PQ TLS test in an Ubuntu Docker container

set -e

echo "Extracting AWS credentials from s3_pq_tls_test profile..."

# Get AWS credentials using AWS CLI from specific profile
export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id --profile default)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key --profile default)

# If session token is empty, unset it
if [ -z "$AWS_SESSION_TOKEN" ]; then
    unset AWS_SESSION_TOKEN
fi

echo "Credentials extracted successfully"
echo "Access Key: ${AWS_ACCESS_KEY_ID:0:10}..."
echo ""
echo "Building Docker image..."
docker build -t pq-tls-test:latest .

echo ""
echo "Running test in Ubuntu Linux container..."
echo "==========================================="
echo ""

# Run the container with:
# - Local Maven repo mounted (for the SNAPSHOT SDK)
# - AWS credentials as environment variables
docker run --rm \
    -v ~/.m2:/root/.m2 \
    -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
    -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
    -e AWS_REGION=us-east-1 \
    pq-tls-test:latest

echo ""
echo "==========================================="
echo "Test completed in Docker container"
