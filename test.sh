#!/usr/bin/env sh
if git diff --name-only master HEAD | grep -q "http-clients/"
then
echo "Found changes in http-clients "
BENCH_MARKS+=(ApiCallHttpClientBenchmark)
BENCH_MARKS+=(ClientCreationBenchmark)
fi
if git diff --name-only master HEAD | grep -q "core/"
then
echo "Found changes in core running ApiCallProtocolBenchmark and ClientCreationBenchmark"
BENCH_MARKS+=(ApiCallProtocolBenchmark)
BENCH_MARKS+=(ClientCreationBenchmark)
fi
echo ${BENCH_MARKS[@]}
java -jar test/sdk-benchmarks/target/benchmarks.jar ${BENCH_MARKS[@]}
