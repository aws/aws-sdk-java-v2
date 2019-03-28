# Sdk Benchmark Harness


This module contains sdk benchmark harness using [JMH].

Each benchmark class has a set of default
JMH configurations tailored to SDK's build job and you might need to
adjust them based on your test environment such as increasing warmup iterations
or measurement time in order to get more reliable data.

There are three ways to run benchmarks.

- Using the executable JAR (Preferred usage per JMH site)
```bash
mvn clean install -P quick -pl :sdk-benchmarks --am

# Run specific benchmark
java -jar target/benchmarks.jar ApacheHttpClientBenchmark

# Run all benchmarks: 3 warm up iterations, 3 benchmark iterations, 1 fork
java -jar target/benchmarks.jar -wi 3 -i 3 -f 1
```

- Using`mvn exec:exec` commands to invoke `BenchmarkRunner` main method
```bash
   mvn clean install -P quick -pl :sdk-benchmarks --am
   mvn clean install -pl :bom-internal
   cd test/sdk-benchmarks
   mvn exec:exec
```   

- From IDE
  
  You can run the main method within each Benchmark class from your IDE. If you are using Eclipse, you might need to
  set up build configurations for JMH annotation, please check [JMH]. Note that the benchmark result from IDE 
  might not be as reliable as the above approaches and is generally not recommended.
  
[JMH]: http://openjdk.java.net/projects/code-tools/jmh/
