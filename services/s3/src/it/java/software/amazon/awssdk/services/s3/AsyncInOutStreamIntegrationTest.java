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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.service.S3BucketUtils;

/**
 * Test InputStream and OutputStream operations on AsyncRequestBody and AsyncResponseTransformer against S3.
 */
public class AsyncInOutStreamIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = S3BucketUtils.temporaryBucketName("async-in-out-stream");

    /**
     * Creates all the test resources for the tests.
     */
    @BeforeClass
    public static void createResources() {
        createBucket(BUCKET);
    }

    /**
     * Releases all resources created in this test.
     */
    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void largeFilePutGet() throws IOException {
        long length = 4 * 1024 * 1024; // 4 MB
        BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(length);
        CompletableFuture<?> response = s3Async.putObject(r -> r.bucket(BUCKET).key("foo"), body);

        try (OutputStream os = body.outputStream()) {
            for (int i = 0; i < length; i++) {
                os.write(i % 255);
            }
        }

        response.join();

        try (ResponseInputStream<GetObjectResponse> is =
                 s3Async.getObject(r -> r.bucket(BUCKET).key("foo"),
                                   AsyncResponseTransformer.toBlockingInputStream())
                        .join()) {

            assertThat(is.response().contentLength()).isEqualTo(length);

            for (int i = 0; i < length; i++) {
                assertThat(is.read()).isEqualTo(i % 255);
            }
        }
    }
}
