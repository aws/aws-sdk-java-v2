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

import static software.amazon.awssdk.s3benchmarks.s3express.S3BenchmarkTestUtils.createBucketSafely;
import static software.amazon.awssdk.s3benchmarks.s3express.S3BenchmarkTestUtils.keyNameFromPrefix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public abstract class BaseJavaS3ClientBenchmark implements Benchmark {
    private static final Logger LOGGER = Logger.loggerFor(BaseJavaS3ClientBenchmark.class);
    private static final Integer DEFAULT_BENCHMARK_ITERATIONS = 3;
    private static final Integer DEFAULT_BUCKETS = 1;
    private static final String DEFAULT_BUCKET_SUFFIX = "standard";
    private static final String S3EXPRESS_BUCKET_PATTERN = "-%s--x-s3";
    protected final BenchmarkConfig benchmarkConfig;
    protected final int iteration;
    protected final List<String> buckets;
    protected final byte[] contents;
    private final S3Client adminClient;


    protected BaseJavaS3ClientBenchmark(BenchmarkConfig config) {
        this.iteration = Validate.getOrDefault(config.iteration(), () -> DEFAULT_BENCHMARK_ITERATIONS);
        this.benchmarkConfig = config;
        this.buckets = new ArrayList<>();
        this.adminClient = S3BenchmarkTestUtils.s3ClientBuilder(config.region())
                                               .credentialsProvider(config.credentialsProvider())
                                               .build();
        this.contents = new byte[1024 * benchmarkConfig.contentLengthInKb()];
    }

    @Override
    public void run() {
        try {
            setup();
            LOGGER.info(() -> "Starting warm up");
            warmUp();
            LOGGER.info(() -> "Run benchmark");
            doRunBenchmark();
        } catch (Exception e) {
            LOGGER.error(() -> "Exception occurred", e);
        } finally {
            LOGGER.info(() -> "Starting clean up");
        }
    }

    protected void setup() {
        int numTestBuckets = benchmarkConfig.numBuckets() != null ? benchmarkConfig.numBuckets() : DEFAULT_BUCKETS;
        boolean useS3Express = benchmarkConfig.useS3Express();
        String bucketNameSuffix = DEFAULT_BUCKET_SUFFIX;
        if (useS3Express) {
            bucketNameSuffix = String.format(S3EXPRESS_BUCKET_PATTERN, benchmarkConfig.az());
        }
        for (int i = 0; i < numTestBuckets; i++) {
            String bucketName = S3BenchmarkTestUtils.getTemporaryBucketName(i, bucketNameSuffix).toLowerCase(Locale.ENGLISH);
            buckets.add(bucketName);
            createBucketSafely(adminClient, bucketName, useS3Express);
        }
    }

    protected void warmUp() throws Exception {
        for (int i = 0; i < 3; i++) {
            sendOneRequest();
            Thread.sleep(500);
        }
    }

    protected abstract void doRunBenchmark();

    protected abstract void sendOneRequest();

    protected String bucketName(int i) {
        return buckets.get(i % buckets.size());
    }

    protected String key(int i) {
        return keyNameFromPrefix(i, benchmarkConfig.contentLengthInKb().toString());
    }
}
