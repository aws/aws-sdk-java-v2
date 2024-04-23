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
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3MultipartJavaBasedStabilityTest extends S3AsyncBaseStabilityTest {
    private static final String BUCKET_NAME = String.format("s3multipartjavabasedstabilitytest%d", System.currentTimeMillis());
    private static final S3AsyncClient multipartJavaBasedClient;

    static {
        multipartJavaBasedClient = S3AsyncClient.builder()
                                                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                .multipartEnabled(true)
                                                .build();
    }

    public S3MultipartJavaBasedStabilityTest() {
        super(multipartJavaBasedClient);
    }

    @BeforeAll
    public static void setup() {
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
        multipartJavaBasedClient.close();
        s3ApacheClient.close();
    }

    @Override
    protected String getTestBucketName() {
        return BUCKET_NAME;
    }
}
