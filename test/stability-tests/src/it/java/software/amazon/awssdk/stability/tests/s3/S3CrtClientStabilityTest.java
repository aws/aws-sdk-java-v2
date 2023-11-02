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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.SystemInfo;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

/**
 * Stability tests for {@link S3CrtAsyncClient}
 */
public class S3CrtClientStabilityTest extends S3BaseStabilityTest {
    private static final String BUCKET_NAME = String.format("s3crthttpclientstabilitytests%d", System.currentTimeMillis());

    private static final int CRT_CLIENT_THREAD_COUNT;

    private static final S3Client s3Client;


    static {
        s3Client = S3Client.builder()
                           .httpClientBuilder(AwsCrtHttpClient.builder()
                                                  .maxConcurrency(CONCURRENCY))
                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                           .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10)))
                           .build();

        // The underlying client has a thread per processor in its default configuration along with a DNS resolver thread.
        CRT_CLIENT_THREAD_COUNT = SystemInfo.getProcessorCount() + 1;

    }

    public S3CrtClientStabilityTest() {
        super(s3Client, CRT_CLIENT_THREAD_COUNT);
    }

    @BeforeAll
    public static void setup() {
        System.setProperty("aws.crt.debugnative", "true");
        s3ApacheClient.createBucket(b -> b.bucket(BUCKET_NAME));
        futureThreadPool = Executors.newFixedThreadPool(CONCURRENCY);
    }

    @AfterAll
    public static void cleanup() {
        try (S3AsyncClient s3NettyClient = S3AsyncClient.builder()
                                     .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                               .maxConcurrency(CONCURRENCY))
                                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                     .build()) {
            deleteBucketAndAllContents(s3NettyClient, BUCKET_NAME);
        }
        s3Client.close();
        s3ApacheClient.close();
        CrtResource.waitForNoResources();
        futureThreadPool.shutdown();
        try {
            futureThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }
    }

    @Override
    protected String getTestBucketName() {
        return BUCKET_NAME;
    }
}
