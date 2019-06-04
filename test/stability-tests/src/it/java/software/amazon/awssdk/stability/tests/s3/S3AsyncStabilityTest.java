/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


import static software.amazon.awssdk.stability.tests.utils.StabilityTestUtils.runAsyncTests;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Logger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    }

    @Test
    @Order(1)
    public void putObject() {
        LOGGER.info(() -> "Starting to test putObject");
        byte[] bytes = RandomStringUtils.randomAlphanumeric(10_000).getBytes();

        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            return s3NettyClient.putObject(b -> b.bucket(bucketName).key(keyName),
                                        AsyncRequestBody.fromBytes(bytes));
        };

        runAsyncTests(future, TOTAL_REQUEST_NUMBER, "S3AsyncStabilityTest.putObject");
    }

    @Test
    @Order(2)
    public void getObject() {
        LOGGER.info(() -> "Starting to test getObject");
        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            Path path = RandomTempFile.randomUncreatedFile().toPath();
            return s3NettyClient.getObject(b -> b.bucket(bucketName).key(keyName), AsyncResponseTransformer.toFile(path));
        };

        runAsyncTests(future, TOTAL_REQUEST_NUMBER, "S3AsyncStabilityTest.getObject");
    }
}
