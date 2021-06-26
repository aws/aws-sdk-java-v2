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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.internal.S3CrtAsyncClient;

/**
 * Stability tests for {@link S3CrtAsyncClient}
 */
public class S3CrtAsyncClientStabilityTest extends S3BaseStabilityTest {
    private static final String BUCKET_NAME = "s3crtasyncclinetstabilitytests" + System.currentTimeMillis();
    private static S3CrtAsyncClient s3CrtAsyncClient;

    static {
        s3CrtAsyncClient = S3CrtAsyncClient.builder()
                                           .region(Region.US_WEST_2)
                                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                           .build();
    }

    public S3CrtAsyncClientStabilityTest() {
        super(s3CrtAsyncClient);
    }

    @BeforeAll
    public static void setup() {
        System.setProperty("aws.crt.debugnative", "true");
        s3ApacheClient.createBucket(b -> b.bucket(BUCKET_NAME));
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
        s3CrtAsyncClient.close();
        s3ApacheClient.close();
        CrtResource.waitForNoResources();
    }

    @Override
    protected String getTestBucketName() {
        return BUCKET_NAME;
    }
}
