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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * The S3BenchmarkRunner adds basic upload/download tests.
 *
 * Improvement suggestions
 * - Add detailed metrics
 * - Add more sophisticated support for test suites
 * - Merge with existing benchmarking tests in this module
 * - Dimensions: clients
 * - Support checksumming
 *
 */
public class S3BenchmarkRunner {
    private static final Logger LOGGER = Logger.loggerFor("S3BenchmarkRunner");

    private static List<MetricPublisher> metricPublishers = new ArrayList<>();

    private S3BenchmarkRunner() {
    }

    public static void runBenchmarks(boolean useS3Express) {
        Region region = Region.US_EAST_1;

        String namespacePrefix = useS3Express ? "Veyron" : "Standard";

        AwsCredentialsProvider credentialsProvider = credentialsProvider();

        CloudWatchAsyncClient cloudwatchClient = CloudWatchAsyncClient.builder()
                                                                      .region(region)
                                                                      .httpClient(NettyNioAsyncHttpClient.create())
                                                                      .credentialsProvider(credentialsProvider)
                                                                      .build();

        S3Client s3Client = S3BenchmarkTestUtils.s3ClientBuilder(region)
                                                .credentialsProvider(credentialsProvider)
                                                .httpClient(ApacheHttpClient.create())
                                                .overrideConfiguration(o -> o.addMetricPublisher(
                                                    metricPublisher(cloudwatchClient,
                                                                    namespacePrefix + "/SmallObject/Apache")))
                                                .build();

        LOGGER.info(() -> "Running small objects benchmark with Apache4 Http Client, 64Kb data, 5 buckets, 200 iterations");
        BenchmarkConfig smallObjectSyncConfig = BenchmarkConfig.builder()
                                                               .region(region)
                                                               .credentialsProvider(credentialsProvider)
                                                               .numBuckets(5)
                                                               .iteration(200)
                                                               .contentLengthInKb(64)
                                                               .useS3Express(useS3Express)
                                                               .az("use1-az5")
                                                               .build();
        S3PutGetDeleteSyncBenchmark syncBenchmark = new S3PutGetDeleteSyncBenchmark(smallObjectSyncConfig, s3Client);
        syncBenchmark.run();
        s3Client.close();

        s3Client = S3BenchmarkTestUtils.s3ClientBuilder(region)
                                                .credentialsProvider(credentialsProvider)
                                                .overrideConfiguration(o -> o.addMetricPublisher(
                                                    metricPublisher(cloudwatchClient,
                                                                    namespacePrefix + "/MediumObject/Apache")))
                                                .build();

        LOGGER.info(() -> "Running medium objects benchmark with Apache4 Http Client, 1024Kb data, 5 buckets, 200 iterations");
        BenchmarkConfig largeObjectSyncConfig = BenchmarkConfig.builder()
                                                               .region(region)
                                                               .credentialsProvider(credentialsProvider)
                                                               .numBuckets(5)
                                                               .iteration(200)
                                                               .contentLengthInKb(1024)
                                                               .useS3Express(useS3Express)
                                                               .az("use1-az5")
                                                               .build();
        syncBenchmark = new S3PutGetDeleteSyncBenchmark(largeObjectSyncConfig, s3Client);
        syncBenchmark.run();

        s3Client.close();

        s3Client = S3BenchmarkTestUtils.s3ClientBuilder(region)
                                       .credentialsProvider(credentialsProvider)
                                       .httpClient(Apache5HttpClient.create())
                                       .overrideConfiguration(o -> o.addMetricPublisher(
                                           metricPublisher(cloudwatchClient,
                                                           namespacePrefix + "/SmallObject/Apache5")))
                                       .build();

        LOGGER.info(() -> "Running small objects benchmark with Apache5 Http Client, 64Kb data, 5 buckets, 200 iterations");
        syncBenchmark = new S3PutGetDeleteSyncBenchmark(smallObjectSyncConfig, s3Client);
        syncBenchmark.run();
        s3Client.close();

        s3Client = S3BenchmarkTestUtils.s3ClientBuilder(region)
                                       .credentialsProvider(credentialsProvider)
                                       .httpClient(Apache5HttpClient.create())
                                       .overrideConfiguration(o -> o.addMetricPublisher(
                                           metricPublisher(cloudwatchClient,
                                                           namespacePrefix + "/MediumObject/Apache5")))
                                       .build();

        LOGGER.info(() -> "Running medium objects benchmark with Apache5 Http Client, 1024Kb data, 5 buckets, 200 iterations");
        syncBenchmark = new S3PutGetDeleteSyncBenchmark(largeObjectSyncConfig, s3Client);
        syncBenchmark.run();
        s3Client.close();


        S3AsyncClient s3AsyncClient = S3BenchmarkTestUtils.s3AsyncClientBuilder(region)
                                                          .credentialsProvider(credentialsProvider)
                                                          .overrideConfiguration(o -> o.addMetricPublisher(
                                                              metricPublisher(cloudwatchClient,
                                                                              namespacePrefix + "/SmallObject/Netty")))
                                                          .httpClient(NettyNioAsyncHttpClient.create())
                                                          .build();
        LOGGER.info(() -> "Running async small objects benchmark, 64Kb data, 5 buckets, 100 iterations");
        BenchmarkConfig smallObjectAsyncConfig = BenchmarkConfig.builder()
                                                                .region(region)
                                                                .credentialsProvider(credentialsProvider)
                                                               .numBuckets(5)
                                                               .iteration(100)
                                                               .contentLengthInKb(64)
                                                               .useS3Express(useS3Express)
                                                               .az("use1-az5")
                                                               .build();
        S3PutGetDeleteAsyncBenchmark asyncBenchmark = new S3PutGetDeleteAsyncBenchmark(smallObjectAsyncConfig, s3AsyncClient);
        asyncBenchmark.run();
        s3AsyncClient.close();

        metricPublishers.forEach(MetricPublisher::close);
    }

    private static AwsCredentialsProvider credentialsProvider() {
        Optional<String> roleArn = BenchmarkSystemSetting.BENCHMARK_TEST_ROLE.getStringValue();
        if (!roleArn.isPresent() || roleArn.get().trim().isEmpty()) {
            throw new RuntimeException(String.format("%s environment variable not defined",
                                                     BenchmarkSystemSetting.BENCHMARK_TEST_ROLE));
        }

        LOGGER.info(() -> String.format("Running benchmarks using role %s", roleArn));

        String runId = BenchmarkSystemSetting.RUN_ID.getStringValueOrThrow();
        return StsAssumeRoleCredentialsProvider.builder()
            .stsClient(StsClient.create())
            .refreshRequest(r -> r.roleArn(roleArn.get()).roleSessionName(runId + "-s3express-perf-testing"))
            .build();
    }

    private static CloudWatchMetricPublisher metricPublisher(CloudWatchAsyncClient cloudwatchClient, String namespace) {
        CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                        .cloudWatchClient(cloudwatchClient)
                                        .namespace(namespace)
                                        .dimensions(CoreMetric.SERVICE_ID,
                                                    CoreMetric.OPERATION_NAME,
                                                    HttpMetric.HTTP_CLIENT_NAME)
                                        .metricLevel(MetricLevel.TRACE)
                                    .build();
        metricPublishers.add(publisher);
        return publisher;
    }

    public static void main(String[] args) {
        LOGGER.info(() -> "Running benchmarks against S3 Express");
        runBenchmarks(true);
        LOGGER.info(() -> "Running benchmarks against Standard S3");
        runBenchmarks(false);
    }
}
