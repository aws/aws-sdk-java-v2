# SDK Standard Benchmarks

This module contains JMH microbenchmarks for the AWS SDK for Java v2, covering
endpoint resolution and serialization/deserialization (serde) across all major
AWS protocol types.

## Building

```bash
mvn clean install -P quick -pl :sdk-standard-benchmarks --am
```

The `--am` flag builds all upstream dependencies (codegen plugin, protocol modules,
etc.). You only need it on the first build or after upstream changes. Subsequent
builds can omit it:

```bash
mvn install -P quick -pl :sdk-standard-benchmarks
```

## Endpoint Resolution Benchmarks

Benchmarks for the standard endpoint resolution pipeline (`ruleParams()` →
`resolveEndpoint()`) for S3 and Lambda. These exercise the same code path that
runs during a real SDK API call.

| Class | Service | Test Cases |
|---|---|---|
| `S3EndpointResolverBenchmark` | S3 | 5 (virtual host, path style, S3 Express, access point ARN, outposts ARN) |
| `LambdaEndpointResolverBenchmark` | Lambda | 2 (standard region, GovCloud with FIPS + DualStack) |

```bash
java -jar target/benchmarks.jar S3EndpointResolverBenchmark
java -jar target/benchmarks.jar LambdaEndpointResolverBenchmark
```

## Serde Benchmarks

Benchmarks for serialization (marshaling) and deserialization (unmarshalling)
across five AWS protocol types. Each protocol has a pair of benchmark classes
parameterized by test case ID via JMH `@Param`.

| Protocol | Marshall Class | Unmarshall Class | Input Cases | Output Cases |
|---|---|---|---|---|
| JSON RPC 1.0 | `JsonRpc10MarshallBenchmark` | `JsonRpc10UnmarshallBenchmark` | 33 | 18 |
| AWS Query | `QueryMarshallBenchmark` | `QueryUnmarshallBenchmark` | 33 | 18 |
| REST JSON | `RestJsonMarshallBenchmark` | `RestJsonUnmarshallBenchmark` | 18 | 14 |
| REST XML | `RestXmlMarshallBenchmark` | `RestXmlUnmarshallBenchmark` | 18 | 14 |
| RPC v2 CBOR | `RpcV2CborMarshallBenchmark` | `RpcV2CborUnmarshallBenchmark` | 33 | 18 |

Serde benchmarks use `@BenchmarkMode(Mode.SampleTime)` instead of `AverageTime`.
SampleTime collects per-invocation latency samples, which gives us percentile
data (p50, p90, p95, p99) needed by the cross-language output schema.

### Running serde benchmarks

```bash
# All serde benchmarks (all protocols, all test cases)
java -jar target/benchmarks.jar ".*serde.*"

# Single protocol — marshall only
java -jar target/benchmarks.jar JsonRpc10MarshallBenchmark

# Single protocol — unmarshall only
java -jar target/benchmarks.jar QueryUnmarshallBenchmark

# Quick smoke test: 1 fork, short warmup/measurement
java -jar target/benchmarks.jar JsonRpc10MarshallBenchmark -f 1 -wi 1 -w 1s -i 1 -r 3s -foe true
```

### Producing the cross-language output JSON

The benchmarks produce standard JMH output. To convert it to the cross-language
`output_schema.json` format used for comparison with other SDK implementations
(Ruby, TypeScript, etc.):

```bash
# 1. Run benchmarks and write JMH results as JSON
java -jar target/benchmarks.jar ".*serde.*" -rf json -rff results.json

# 2. Convert to cross-language format
java -cp target/benchmarks.jar \
  software.amazon.awssdk.benchmark.serde.JmhResultConverter \
  results.json output.json
```

The output JSON has this structure:

```json
{
  "metadata": {
    "lang": "Java",
    "software": [["smithy-java", "TODO"], ["AWS SDK for Java", "TODO"]],
    "os": "TODO",
    "instance": "TODO",
    "precision": "-9"
  },
  "serde_benchmarks": [
    {
      "id": "awsJson1_0_GetItemInput_Baseline",
      "n": 5,
      "mean": 1234,
      "p50": 1200,
      "p90": 1500,
      "p95": 1600,
      "p99": 1800,
      "std_dev": 150
    }
  ]
}
```

The `metadata` fields marked `TODO` should be filled in for the target environment
before publishing results.

## General JMH options

```bash
# List all available benchmarks
java -jar target/benchmarks.jar -l

# Custom warmup/measurement: 3 warmup iterations, 5 measurement iterations, 2 forks
java -jar target/benchmarks.jar -wi 3 -i 5 -f 2

# Custom warmup/measurement times: 
java -jar target/benchmarks.jar -f 1 -w 1s -r 3s 

# Fail on error (useful for CI)
java -jar target/benchmarks.jar -foe true
```

Each benchmark class has default JMH annotations (`@Warmup`, `@Measurement`, `@Fork`)
tailored for stable results. Override them on the command line as needed for your
environment.
