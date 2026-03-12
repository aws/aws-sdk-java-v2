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

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.testutils.retry.RetryableTest;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.utils.Logger;

public abstract class S3MockStabilityTestBase {

    protected static final int CONCURRENCY = 100;
    protected static final int TOTAL_RUNS = 50;
    private static Set<ChecksumAlgorithm> CHECKSUM_ALGORITHMS = ChecksumAlgorithm.knownValues();
    private static final Logger log = Logger.loggerFor(S3MockStabilityTestBase.class);
    MockAsyncHttpClient mockAsyncHttpClient;
    S3AsyncClient testClient;
    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void putObject_Checksum() {
        putObjectChecksumVariations();
    }

    protected void putObjectChecksumVariations() {
        mockAsyncHttpClient.stubNextResponse200();
        byte[] bytes = RandomStringUtils.randomAlphanumeric(10_000).getBytes();

        IntFunction<CompletableFuture<?>> future = i -> {
            String keyName = computeKeyName(i);
            ChecksumAlgorithm checksumAlgorithm = i % 2 == 0 ? randomChecksumAlgorithm() : null;
            return testClient.putObject(b -> b.bucket(getTestBucketName())
                                              .key(keyName)
                                              .checksumAlgorithm(checksumAlgorithm), AsyncRequestBody.fromBytes(bytes));
        };

        StabilityTestRunner.newRunner()
                           .testName("S3MockStabilityTestBase.putObjectChecksumVariations")
                           .futureFactory(future)
                           .requestCountPerRun(CONCURRENCY)
                           .totalRuns(TOTAL_RUNS)
                           .delaysBetweenEachRun(Duration.ofMillis(100))
                           .run();
    }

    protected String computeKeyName(int i) {
        return "key_" + i;
    }

    protected abstract String getTestBucketName();

    private static ChecksumAlgorithm randomChecksumAlgorithm() {
        return CHECKSUM_ALGORITHMS.stream()
                                  .skip((int) (Math.random() * CHECKSUM_ALGORITHMS.size()))
                                  .findFirst()
                                  .orElse(null);
    }
}
