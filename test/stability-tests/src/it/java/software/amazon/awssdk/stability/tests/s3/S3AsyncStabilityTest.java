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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Logger;

public abstract class S3AsyncStabilityTest extends S3BaseStabilityTest {
    private static final Logger LOGGER = Logger.loggerFor(S3AsyncStabilityTest.class);

    protected abstract S3AsyncClient getTestClient();

    protected abstract String getTestBucketName();

    protected void doGetBucketAcl_lowTpsLongInterval() {
        IntFunction<CompletableFuture<?>> future = i -> getTestClient().getBucketAcl(b -> b.bucket(getTestBucketName()));
        String className = this.getClass().getSimpleName();
        StabilityTestRunner.newRunner()
                .testName(className + ".getBucketAcl_lowTpsLongInterval")
                .futureFactory(future)
                .requestCountPerRun(10)
                .totalRuns(3)
                .delaysBetweenEachRun(Duration.ofSeconds(6))
                .run();
    }


    protected void downloadLargeObjectToFile() {
        File randomTempFile = RandomTempFile.randomUncreatedFile();
        StabilityTestRunner.newRunner()
                           .testName("S3AsyncStabilityTest.downloadLargeObjectToFile")
                           .futures(getTestClient().getObject(b -> b.bucket(getTestBucketName()).key(LARGE_KEY_NAME),
                                                            AsyncResponseTransformer.toFile(randomTempFile)))
                           .run();
        randomTempFile.delete();
    }

    protected void uploadLargeObjectFromFile() {
        RandomTempFile file = null;
        try {
            file = new RandomTempFile((long) 2e+9);
            StabilityTestRunner.newRunner()
                               .testName("S3AsyncStabilityTest.uploadLargeObjectFromFile")
                               .futures(getTestClient().putObject(b -> b.bucket(getTestBucketName()).key(LARGE_KEY_NAME),
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

    protected void putObject() {
        byte[] bytes = RandomStringUtils.randomAlphanumeric(10_000).getBytes();

        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            return getTestClient().putObject(b -> b.bucket(getTestBucketName()).key(keyName),
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
    
    protected AtomicInteger futuresCreated = new AtomicInteger(0);
    protected AtomicInteger futuresCompleted = new AtomicInteger(0);

    protected void getObject() {
        LOGGER.info(() -> "Starting to test getObject");
        IntFunction<CompletableFuture<?>> future = i -> {
            int createCount = futuresCreated.incrementAndGet();
            LOGGER.info(() -> String.format("Created %d futures", createCount));

            String keyName = computeKeyName(i);
            Path path = RandomTempFile.randomUncreatedFile().toPath();
            CompletableFuture<?> getFuture = getTestClient().getObject(b -> b.bucket(getTestBucketName()).key(keyName), AsyncResponseTransformer.toFile(path));
            return getFuture.whenComplete((res, throwable) -> {
                int completeCount = futuresCompleted.incrementAndGet();
                LOGGER.info(() -> String.format("Completed %d futures", completeCount));
            });
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
