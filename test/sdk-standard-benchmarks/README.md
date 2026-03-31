# SDK Standard Benchmarks

This module contains JMH benchmarks for the standard endpoint resolution pipeline
(`ruleParams()` → `resolveEndpoint()`) for S3 and Lambda services.

Unlike `sdk-benchmarks`, these benchmarks exercise the full standard pipeline — the
same code path that runs during a real SDK API call — including parameter extraction
from `ExecutionAttributes` and the `SdkRequest`.

The package structure (`software.amazon.awssdk.benchmark.endpoints`) supports both
JMH benchmarks and standalone `main`-method benchmarks. Non-JMH classes can be run
via `mvn exec:exec` or direct `java -cp` invocation.

## Benchmark Classes

| Class | Service | Test Cases |
|---|---|---|
| `S3EndpointResolverBenchmark` | S3 | 5 cases (virtual host, path style, S3 Express, access point ARN, outposts ARN) |
| `LambdaEndpointResolverBenchmark` | Lambda | 2 cases (standard region, GovCloud with FIPS + DualStack) |

## Building

```bash
mvn clean install -P quick -pl :sdk-standard-benchmarks --am
```

## Running Benchmarks

Using the executable JAR (preferred per JMH site):

```bash
# Run S3 benchmark
java -jar test/sdk-standard-benchmarks/target/benchmarks.jar S3EndpointResolverBenchmark

# Run Lambda benchmark
java -jar target/benchmarks.jar LambdaEndpointResolverBenchmark

# Run all benchmarks with custom JMH options: 3 warmup iterations, 3 measurement iterations, 1 fork
java -jar target/benchmarks.jar -wi 3 -i 3 -f 1
```

Each benchmark class has default JMH configurations tailored to the SDK's build job.
You may need to adjust warmup iterations or measurement time for your test environment
to get more reliable data.
