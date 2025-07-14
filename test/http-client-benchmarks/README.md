````markdown
# HTTP Client Benchmark Harness

This module contains HTTP client benchmark harness using [JMH].

Each benchmark class has a set of default
JMH configurations tailored to HTTP client performance testing and you might need to
adjust them based on your test environment such as increasing warmup iterations
or measurement time in order to get more reliable data.

There are three ways to run benchmarks.

- Using the executable JAR (Preferred usage per JMH site)
```bash
mvn clean install -P quick -pl :http-client-benchmarks --am

# Run specific benchmark
java -jar target/benchmarks.jar Apache5Benchmark

# Run all benchmarks: 3 warm up iterations, 3 benchmark iterations, 1 fork
java -jar target/benchmarks.jar -wi 3 -i 3 -f 1
```

- Using `mvn exec:exec` commands to invoke `UnifiedBenchmarkRunner` main method
```bash
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

The runner executes all benchmark variations, publishes metrics to CloudWatch, and generates a detailed comparison report showing performance improvements between implementations.

## Benchmark Operations

Each benchmark implementation tests the following operations:
- `simpleGet`: Single-threaded GET operations
- `simplePut`: Single-threaded PUT operations 
- `multiThreadedGet`: Multi-threaded GET operations (10 threads)
- `multiThreadedPut`: Multi-threaded PUT operations (10 threads)

[JMH]: http://openjdk.java.net/projects/code-tools/jmh/
````