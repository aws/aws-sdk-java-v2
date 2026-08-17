# BDD Endpoints Benchmarks

JMH microbenchmarks comparing endpoint resolution strategies across three services
(Connect, DynamoDB, S3) and four resolver implementations:

| Resolver | Description |
|---|---|
| `rules` | SDK v2 rules-based resolver (no BDD, baseline) |
| `baselineBdd` | SDK v2 original table-driven BDD |
| `optimizedBdd` | SDK v2 optimized inlined-branch BDD |
| `smithyJava` | smithy-java `BytecodeEndpointResolver` compiled from the smithy model |

## Building

```bash
# First build — includes all upstream dependencies
mvn clean install -P quick -pl :bdd-endpoints-benchmarks --am

# Subsequent builds
mvn install -P quick -pl :bdd-endpoints-benchmarks
```

## Running

```bash
# All benchmarks (default settings: 4 forks, 2 warmup × 10s, 5 measurement × 30s)
java -jar test/bdd-endpoints-benchmarks/target/benchmarks.jar

# Single service
java -jar test/bdd-endpoints-benchmarks/target/benchmarks.jar S3EndpointBenchmark

# Single resolver across all services
java -jar test/bdd-endpoints-benchmarks/target/benchmarks.jar -p resolver=optimizedBdd

# Quick smoke test (useful for local validation)
java -jar test/bdd-endpoints-benchmarks/target/benchmarks.jar \
  -wi 1 -w 1s -i 1 -r 3s -f 1 -tu ns -foe true

# Write results to JSON for post-processing
java -jar test/bdd-endpoints-benchmarks/target/benchmarks.jar -rf json -rff results.json
```

## Benchmarks

Each service benchmark has:
- **`aggregate`** — all cases shuffled per iteration (measures mixed workload throughput)
- **`caseN_*`** — individual cases run in isolation (lets the JIT specialize each path)

| Class | Cases |
|---|---|
| `ConnectEndpointBenchmark` | regional, custom endpoint, FIPS, FIPS+dual-stack, EU dual-stack, CN dual-stack |
| `DynamoDbEndpointBenchmark` | regional, FIPS+dual-stack, account-ID preferred, account-ID CN fallback, custom endpoint |
| `S3EndpointBenchmark` | virtual addressing, path style, S3 Express data plane, access point ARN, Outposts |

## Deploying to EC2 for production runs

```bash
# Copy the fat jar to the benchmark instance
scp -i benchmark-ec2.pem \
  test/bdd-endpoints-benchmarks/target/benchmarks.jar \
  ec2-user@<host>:~/

# Run on the instance (recommended settings for stable results)
java -jar benchmarks.jar \
  -wi 3 -w 2s \
  -i 5 -r 5s \
  -f 4 \
  -tu ns
```
