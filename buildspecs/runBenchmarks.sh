#!/usr/bin/env bash
cd test/sdk-benchmarks
BENCH_MARKS=()
if git diff --name-only master HEAD | grep -q "http-clients/"; then
    echo "Found changes in http-clients module, will run ApiCallHttpClientBenchmark and ClientCreationBenchmark"
    BENCH_MARKS+=(ApiCallHttpClientBenchmark.run.UrlConnectionHttpClient:$URL_CONNECTION_CLIENT_BASELINE_SCORE)
    BENCH_MARKS+=(ApiCallHttpClientBenchmark.run.ApacheHttpClient:$APACHE_CLIENT_BASELINE_SCORE)
fi

if git diff --name-only master HEAD | grep -q "core/"; then
    echo "Found changes in core, will run ApiCallProtocolBenchmark and ClientCreationBenchmark"
    BENCH_MARKS+=(ApiCallProtocolBenchmark.run.xml:$XML_BASELINE_SCORE)
    BENCH_MARKS+=(ApiCallProtocolBenchmark.run.json:$JSON_BASELINE_SCORE)
    BENCH_MARKS+=(ApiCallProtocolBenchmark.run.ec2:$EC2_BASELINE_SCORE)
    BENCH_MARKS+=(ApiCallProtocolBenchmark.run.query:$QUERY_BASELINE_SCORE)
    BENCH_MARKS+=(ClientCreationBenchmark.run:$CLIENT_CREATION_BASELINE_SCORE)
fi

if [ ${#BENCH_MARKS[@]} -eq 0 ]; then
    echo "No benchmarks to run"
else
    echo "Running the following benchmarks: ${BENCH_MARKS[@]}"
    ARGS=$(printf ",%s" "${BENCH_MARKS[@]}")
    ARGS=${ARGS:1}
    mvn exec:exec -Dbenchmarks=$ARGS
fi