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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3MultipartJavaBasedStabilityTest extends S3AsyncBaseStabilityTest {
    private static final String BUCKET_NAME = String.format("s3multipartjavabasedstabilitytest%d", System.currentTimeMillis());
    private static final S3AsyncClient multipartJavaBasedClient;

    static {
        multipartJavaBasedClient = S3AsyncClient.builder()
                                                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                                          .maxConcurrency(CONCURRENCY))
                                                .multipartEnabled(true)
                                                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(5))
                                                                             // Retry at test level
                                                                             .retryPolicy(RetryPolicy.none()))
                                                .build();
    }

    public S3MultipartJavaBasedStabilityTest() {
        // S3 multipart client uses more threads because for large file uploads, it reads from different positions of the files
        // at the same time, which will trigger more Java I/O threads to spin up
        super(multipartJavaBasedClient, 250);
    }

    @BeforeAll
    public static void setup() {
        multipartJavaBasedClient.createBucket(b -> b.bucket(BUCKET_NAME)).join();
        multipartJavaBasedClient.waiter().waitUntilBucketExists(b -> b.bucket(BUCKET_NAME)).join();
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(multipartJavaBasedClient, BUCKET_NAME);
        multipartJavaBasedClient.close();
    }

    @Override
    protected String getTestBucketName() {
        return BUCKET_NAME;
    }
}
