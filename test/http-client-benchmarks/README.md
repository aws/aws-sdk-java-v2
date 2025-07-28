# HTTP Client Benchmark Harness

This module contains HTTP client benchmark harness using [JMH].

Each benchmark class has a set of default
JMH configurations tailored to HTTP client performance testing and you might need to
adjust them based on your test environment such as increasing warmup iterations
or measurement time in order to get more reliable data.

There are three ways to run benchmarks.

- Using the executable JAR (Preferred usage per JMH site)
```
  mvn clean install -P quick -pl :http-client-benchmarks --am
```
  
# Run specific benchmark
```
java -jar target/http-client-benchmarks.jar Apache5Benchmark
```

# Run all benchmarks: 3 warm up iterations, 3 benchmark iterations, 1 fork.
```
java -jar target/http-client-benchmarks.jar -wi 3 -i 3 -f 1
```

- Using `mvn exec:exec` commands to invoke `UnifiedBenchmarkRunner` main method
```
  mvn clean install -P quick -pl :http-client-benchmarks --am
  mvn clean install -pl :bom-internal
  cd test/http-client-benchmarks
  mvn exec:exec
```

## UnifiedBenchmarkRunner

The `UnifiedBenchmarkRunner` provides a comprehensive comparison between different HTTP client implementations:

- **Apache4**: Apache HttpClient 4.x baseline
- **Apache5-Platform**: Apache HttpClient 5.x with platform threads
- **Apache5-Virtual**: Apache HttpClient 5.x with virtual threads

The runner executes all benchmark variations, prints metrics to console, and publishes results to CloudWatch metrics for monitoring and analysis.

## Benchmark Operations

Each benchmark implementation tests the following operations:
- `simpleGet`: Single-threaded GET operations
- `simplePut`: Single-threaded PUT operations
- `multiThreadedGet`: Multi-threaded GET operations (10 threads)
- `multiThreadedPut`: Multi-threaded PUT operations (10 threads)

## Prerequisites

### Java Runtime Requirements

- **Java 8+**: Required for running the benchmarks (as specified by `<javac.target>8</javac.target>`)
- **Java 21+**: Required for virtual threads support (Apache5-Virtual benchmarks)

**Note**: Virtual threads are a preview feature in Java 19-20 and became stable in Java 21. The Apache5-Virtual benchmarks require Java 21 or later.

