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

package software.amazon.awssdk.stability.tests.s3;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Logger;

public class S3AsyncStabilityTest extends S3BaseStabilityTest {
    private static final Logger LOGGER = Logger.loggerFor(S3AsyncStabilityTest.class);
    private static String bucketName = "s3asyncstabilitytests" + System.currentTimeMillis();

    @BeforeAll
    public static void setup() {
        s3NettyClient.createBucket(b -> b.bucket(bucketName)).join();
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(bucketName);
        s3NettyClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    @Override
    public void putObject_getObject_highConcurrency() {
        putObject();
        getObject();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void largeObject_put_get_usingFile() {
        uploadLargeObjectFromFile();
        downloadLargeObjectToFile();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void getBucketAcl_lowTpsLongInterval() {
        IntFunction<CompletableFuture<?>> future = i -> s3NettyClient.getBucketAcl(b -> b.bucket(bucketName));
        StabilityTestRunner.newRunner()
                           .testName("S3AsyncStabilityTest.getBucketAcl_lowTpsLongInterval")
                           .futureFactory(future)
                           .requestCountPerRun(10)
                           .totalRuns(3)
                           .delaysBetweenEachRun(Duration.ofSeconds(6))
                           .run();
    }

    private void downloadLargeObjectToFile() {
        File randomTempFile = RandomTempFile.randomUncreatedFile();
        StabilityTestRunner.newRunner()
                           .testName("S3AsyncStabilityTest.downloadLargeObjectToFile")
                           .futures(s3NettyClient.getObject(b -> b.bucket(bucketName).key(LARGE_KEY_NAME),
                                                            AsyncResponseTransformer.toFile(randomTempFile)))
                           .run();
        randomTempFile.delete();
    }

    private void uploadLargeObjectFromFile() {
        RandomTempFile file = null;
        try {
            file = new RandomTempFile((long) 2e+9);
            StabilityTestRunner.newRunner()
                               .testName("S3AsyncStabilityTest.uploadLargeObjectFromFile")
                               .futures(s3NettyClient.putObject(b -> b.bucket(bucketName).key(LARGE_KEY_NAME),
                                                                AsyncRequestBody.fromFile(file)))
                               .run();
        } catch (IOException e) {
            throw new RuntimeException("fail to create test file", e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    private void putObject() {
        LOGGER.info(() -> "Starting to test putObject");
        byte[] bytes = RandomStringUtils.randomAlphanumeric(10_000).getBytes();

        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            return s3NettyClient.putObject(b -> b.bucket(bucketName).key(keyName),
                                        AsyncRequestBody.fromBytes(bytes));
        };

        StabilityTestRunner.newRunner()
                           .testName("S3AsyncStabilityTest.putObject")
                           .futureFactory(future)
                           .requestCountPerRun(CONCURRENCY)
                           .totalRuns(TOTAL_RUNS)
                           .delaysBetweenEachRun(Duration.ofMillis(100))
                           .run();
    }

    private void getObject() {
        LOGGER.info(() -> "Starting to test getObject");
        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            Path path = RandomTempFile.randomUncreatedFile().toPath();
            return s3NettyClient.getObject(b -> b.bucket(bucketName).key(keyName), AsyncResponseTransformer.toFile(path));
        };

        StabilityTestRunner.newRunner()
                           .testName("S3AsyncStabilityTest.getObject")
                           .futureFactory(future)
                           .requestCountPerRun(CONCURRENCY)
                           .totalRuns(TOTAL_RUNS)
                           .delaysBetweenEachRun(Duration.ofMillis(100))
                           .run();
    }
}
