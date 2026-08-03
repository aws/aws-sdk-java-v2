/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.s3benchmarks.s3express;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.Logger;

/**
 * Runs S3 benchmarks against both S3 Express One Zone (directory buckets) and standard S3,
 * outputting results as JMH-compatible JSON.
 */
public class S3BenchmarkRunner {
    private static final Logger LOGGER = Logger.loggerFor("S3BenchmarkRunner");
    private static final Region REGION = Region.US_WEST_2;
    private static final String AZ = "usw2-az1";

    private S3BenchmarkRunner() {
    }

    public static void main(String[] args) throws IOException {
        String outputFile = parseOutputFile(args);

        List<Map<String, Object>> results = new ArrayList<>();

        LOGGER.info(() -> "Running benchmarks against S3 Express");
        results.addAll(runBenchmarks(true));
        LOGGER.info(() -> "Running benchmarks against Standard S3");
        results.addAll(runBenchmarks(false));

        writeResultsJson(results, outputFile);
        LOGGER.info(() -> "Results written to " + outputFile);
    }

    private static String parseOutputFile(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--output".equals(args[i]) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return "results.json";
    }

    private static List<Map<String, Object>> runBenchmarks(boolean useS3Express) {
        List<Map<String, Object>> results = new ArrayList<>();
        String bucketType = useS3Express ? "s3express" : "standard";

        InMemoryMetricPublisher metricPublisher = new InMemoryMetricPublisher();
        DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

        S3Client syncClient = S3BenchmarkTestUtils.s3ClientBuilder(REGION)
                                                  .credentialsProvider(credentialsProvider)
                                                  .overrideConfiguration(o -> o.addMetricPublisher(metricPublisher))
                                                  .build();

        try {
            results.addAll(runSyncBenchmark(syncClient, metricPublisher, credentialsProvider, useS3Express, 64, bucketType));
            results.addAll(runSyncBenchmark(syncClient, metricPublisher, credentialsProvider, useS3Express, 1024, bucketType));
        } finally {
            syncClient.close();
        }

        S3AsyncClient asyncClient = S3AsyncClient.builder()
                                                 .region(REGION)
                                                 .credentialsProvider(credentialsProvider)
                                                 .overrideConfiguration(o -> o.addMetricPublisher(metricPublisher))
                                                 .httpClient(NettyNioAsyncHttpClient.create())
                                                 .build();
        try {
            results.addAll(runAsyncBenchmark(asyncClient, metricPublisher, credentialsProvider, useS3Express, 64, bucketType));
        } finally {
            asyncClient.close();
        }

        metricPublisher.close();
        return results;
    }

    private static List<Map<String, Object>> runSyncBenchmark(S3Client s3Client,
                                                              InMemoryMetricPublisher metricPublisher,
                                                              DefaultCredentialsProvider credentialsProvider,
                                                              boolean useS3Express,
                                                              int contentLengthInKb, String bucketType) {
        String objectSize = contentLengthInKb >= 1024 ? (contentLengthInKb / 1024) + "MB" : contentLengthInKb + "KB";
        LOGGER.info(() -> String.format("Running sync benchmark: %s, %s, %d buckets, 200 iterations",
                                        bucketType, objectSize, 5));

        BenchmarkConfig config = BenchmarkConfig.builder()
                                                .region(REGION)
                                                .credentialsProvider(credentialsProvider)
                                                .numBuckets(5)
                                                .iteration(200)
                                                .contentLengthInKb(contentLengthInKb)
                                                .useS3Express(useS3Express)
                                                .az(AZ)
                                                .build();

        S3PutGetDeleteSyncBenchmark benchmark = new S3PutGetDeleteSyncBenchmark(config, s3Client, metricPublisher);
        benchmark.run();

        List<Map<String, Object>> results = new ArrayList<>();
        results.add(buildResult("s3express.S3Benchmark.putObject", metricPublisher.avgDurationMs("PutObject"),
                                "ms/op", bucketType, objectSize, "Apache"));
        results.add(buildResult("s3express.S3Benchmark.getObject", metricPublisher.avgDurationMs("GetObject"),
                                "ms/op", bucketType, objectSize, "Apache"));
        results.add(buildResult("s3express.S3Benchmark.deleteObject", metricPublisher.avgDurationMs("DeleteObject"),
                                "ms/op", bucketType, objectSize, "Apache"));
        results.add(buildResult("s3express.S3Benchmark.readThroughput", metricPublisher.avgReadThroughput("GetObject"),
                                "bytes/s", bucketType, objectSize, "Apache"));
        return results;
    }

    private static List<Map<String, Object>> runAsyncBenchmark(S3AsyncClient s3AsyncClient,
                                                               InMemoryMetricPublisher metricPublisher,
                                                               DefaultCredentialsProvider credentialsProvider,
                                                               boolean useS3Express,
                                                               int contentLengthInKb, String bucketType) {
        String objectSize = contentLengthInKb >= 1024 ? (contentLengthInKb / 1024) + "MB" : contentLengthInKb + "KB";
        LOGGER.info(() -> String.format("Running async benchmark: %s, %s, %d buckets, 100 iterations",
                                        bucketType, objectSize, 5));

        BenchmarkConfig config = BenchmarkConfig.builder()
                                                .region(REGION)
                                                .credentialsProvider(credentialsProvider)
                                                .numBuckets(5)
                                                .iteration(100)
                                                .contentLengthInKb(contentLengthInKb)
                                                .useS3Express(useS3Express)
                                                .az(AZ)
                                                .build();

        S3PutGetDeleteAsyncBenchmark benchmark = new S3PutGetDeleteAsyncBenchmark(config, s3AsyncClient, metricPublisher);
        benchmark.run();

        List<Map<String, Object>> results = new ArrayList<>();
        results.add(buildResult("s3express.S3Benchmark.putObject", metricPublisher.avgDurationMs("PutObject"),
                                "ms/op", bucketType, objectSize, "Netty"));
        results.add(buildResult("s3express.S3Benchmark.getObject", metricPublisher.avgDurationMs("GetObject"),
                                "ms/op", bucketType, objectSize, "Netty"));
        results.add(buildResult("s3express.S3Benchmark.deleteObject", metricPublisher.avgDurationMs("DeleteObject"),
                                "ms/op", bucketType, objectSize, "Netty"));
        results.add(buildResult("s3express.S3Benchmark.readThroughput", metricPublisher.avgReadThroughput("GetObject"),
                                "bytes/s", bucketType, objectSize, "Netty"));
        return results;
    }

    private static Map<String, Object> buildResult(String benchmarkName, double score, String scoreUnit,
                                                   String bucketType, String objectSize, String httpClient) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("benchmark", benchmarkName);
        result.put("mode", "avgt");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("bucketType", bucketType);
        params.put("objectSize", objectSize);
        params.put("httpClient", httpClient);
        result.put("params", params);

        Map<String, Object> primaryMetric = new LinkedHashMap<>();
        primaryMetric.put("score", score);
        primaryMetric.put("scoreUnit", scoreUnit);

        List<List<Double>> rawData = new ArrayList<>();
        List<Double> fork = new ArrayList<>();
        fork.add(score);
        rawData.add(fork);
        primaryMetric.put("rawData", rawData);

        result.put("primaryMetric", primaryMetric);
        return result;
    }

    private static void writeResultsJson(List<Map<String, Object>> results, String outputFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(Paths.get(outputFile).toFile(), results);
    }
}
