#!/usr/bin/env bash
#
# Runs the Enhanced Query (join and aggregation) benchmark against in-process DynamoDB Local.
# No AWS credentials or EC2 required. Creates and seeds 1000 customers x 1000 orders, then runs
# five scenarios and prints latency stats (avgMs, p50Ms, p95Ms, rows).
#
# Usage: from the repository root, run:
#   ./services-custom/dynamodb-enhanced/run-enhanced-query-benchmark-local.sh
#
# Optional environment variables (before running the script):
#   BENCHMARK_ITERATIONS  – measured runs per scenario (default: 5)
#   BENCHMARK_WARMUP      – warm-up runs per scenario (default: 2)
#   BENCHMARK_OUTPUT_FILE – if set, CSV results are appended to this path
#
# Example with custom iterations and saving results:
#   BENCHMARK_ITERATIONS=10 BENCHMARK_OUTPUT_FILE=benchmark_local.csv \
#     ./services-custom/dynamodb-enhanced/run-enhanced-query-benchmark-local.sh
#

set -e
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

export USE_LOCAL_DYNAMODB=true
# Match surefire: avoid DynamoDB Local telemetry (Pinpoint) when not needed; also satisfied by url-connection-client on test classpath
export DDB_LOCAL_TELEMETRY=0
# Optional: override iterations/warmup/output (defaults are in the runner)
# export BENCHMARK_ITERATIONS=5
# export BENCHMARK_WARMUP=2
# export BENCHMARK_OUTPUT_FILE=benchmark_local.csv

echo "Running Enhanced Query benchmark (DynamoDB Local, 1000 customers x 1000 orders)..."
mvn test-compile exec:java -pl services-custom/dynamodb-enhanced \
  -Dexec.mainClass="software.amazon.awssdk.enhanced.dynamodb.functionaltests.EnhancedQueryBenchmarkRunner" \
  -Dexec.classpathScope=test \
  -Dspotbugs.skip=true

echo "Done. Results are printed above."
