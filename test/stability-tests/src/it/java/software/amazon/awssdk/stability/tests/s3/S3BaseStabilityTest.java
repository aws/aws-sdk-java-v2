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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

import org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

public abstract class S3BaseStabilityTest extends AwsTestBase {
    private static final Logger log = Logger.loggerFor(S3BaseStabilityTest.class);
    protected static final int CONCURRENCY = 100;
    protected static final int TOTAL_RUNS = 50;
    protected static final String LARGE_KEY_NAME = "2GB";

    protected static S3Client s3ApacheClient;

    static {
        s3ApacheClient = S3Client.builder()
                                 .httpClientBuilder(ApacheHttpClient.builder()
                                                                    .maxConnections(CONCURRENCY))
                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                 .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10)))
                                 .build();
    }

    protected String computeKeyName(int i) {
        return "key_" + i;
    }

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

    protected void getObject() {
        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            Path path = RandomTempFile.randomUncreatedFile().toPath();
            return getTestClient().getObject(b -> b.bucket(getTestBucketName()).key(keyName), AsyncResponseTransformer.toFile(path));
        };

        StabilityTestRunner.newRunner()
                .testName("S3AsyncStabilityTest.getObject")
                .futureFactory(future)
                .requestCountPerRun(CONCURRENCY)
                .totalRuns(TOTAL_RUNS)
                .delaysBetweenEachRun(Duration.ofMillis(100))
                .run();
    }

    protected static void deleteBucketAndAllContents(S3AsyncClient client, String bucketName) {
        try {
            List<CompletableFuture<?>> futures = new ArrayList<>();

            client.listObjectsV2Paginator(b -> b.bucket(bucketName))
                         .subscribe(r -> r.contents().forEach(s -> futures.add(client.deleteObject(o -> o.bucket(bucketName).key(s.key())))))
                         .join();

            CompletableFuture<?>[] futureArray = futures.toArray(new CompletableFuture<?>[0]);

            CompletableFuture.allOf(futureArray).join();

            client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join();
        } catch (Exception e) {
            log.error(() -> "Failed to delete bucket: " +bucketName);
        }
    }

    protected void verifyObjectExist(String bucketName, String keyName, long size) throws IOException {
        try {
            s3ApacheClient.headBucket(b -> b.bucket(bucketName));
        } catch (NoSuchBucketException e) {
            log.info(() -> "NoSuchBucketException was thrown, staring to create the bucket");
            s3ApacheClient.createBucket(b -> b.bucket(bucketName));
        }

        try {
            s3ApacheClient.headObject(b -> b.key(keyName).bucket(bucketName));
        } catch (NoSuchKeyException e) {
            log.info(() -> "NoSuchKeyException was thrown, starting to upload the object");
            RandomTempFile file = new RandomTempFile(size);
            s3ApacheClient.putObject(b -> b.bucket(bucketName).key(keyName), RequestBody.fromFile(file));
            file.delete();
        }
    }

}
