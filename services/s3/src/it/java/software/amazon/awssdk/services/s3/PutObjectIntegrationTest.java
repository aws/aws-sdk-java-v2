
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
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.CHECKSUM;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Integration tests for {@code PutObject}.
 */
public class PutObjectIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(PutObjectIntegrationTest.class);
    private static final String ASYNC_KEY = "async-key";
    private static final String SYNC_KEY = "sync-key";
    private static final String TEXT_CONTENT_TYPE = "text/plain";
    private static final byte[] CONTENT = "Hello".getBytes(StandardCharsets.UTF_8);

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void objectInputStreamsAreClosed() {
        TestContentProvider provider = new TestContentProvider(CONTENT);
        s3.putObject(r -> r.bucket(BUCKET).key(SYNC_KEY),
                     RequestBody.fromContentProvider(provider, CONTENT.length, "binary/octet-stream"));

        for (CloseTrackingInputStream is : provider.getCreatedStreams()) {
            assertThat(is.isClosed()).isTrue();
        }
    }

    @Test
    public void blockingInputStreamAsyncRequestBody_withContentType_isHonored() {
        BlockingInputStreamAsyncRequestBody requestBody =
            BlockingInputStreamAsyncRequestBody.builder()
                                               .contentLength((long) CONTENT.length)
                                               .contentType(TEXT_CONTENT_TYPE)
                                               .build();

        PutObjectRequest.Builder request = PutObjectRequest.builder()
                                                           .bucket(BUCKET)
                                                           .key(ASYNC_KEY);

        CompletableFuture<PutObjectResponse> responseFuture = s3Async.putObject(request.build(), requestBody);
        requestBody.writeInputStream(new ByteArrayInputStream(CONTENT));
        responseFuture.join();

        HeadObjectResponse response = s3Async.headObject(r -> r.bucket(BUCKET).key(ASYNC_KEY)).join();

        assertThat(response.contentLength()).isEqualTo(CONTENT.length);
        assertThat(response.contentType()).isEqualTo(TEXT_CONTENT_TYPE);
    }

    @Test
    public void s3Client_usingHttpAndDisableChunkedEncoding() {
        try (S3Client s3Client = s3ClientBuilder()
            .endpointOverride(URI.create("http://s3.us-west-2.amazonaws.com"))
            .serviceConfiguration(S3Configuration.builder()
                                                 .chunkedEncodingEnabled(false)
                                                 .build())
            .build()) {
            assertThat(s3Client.putObject(b -> b.bucket(BUCKET).key(SYNC_KEY), RequestBody.fromBytes(
                "helloworld".getBytes()))).isNotNull();
        }
    }

    private static class TestContentProvider implements ContentStreamProvider {
        private final byte[] content;
        private final List<CloseTrackingInputStream> createdStreams = new ArrayList<>();
        private CloseTrackingInputStream currentStream;

        private TestContentProvider(byte[] content) {
            this.content = content;
        }

        @Override
        public InputStream newStream() {
            if (currentStream != null) {
                invokeSafely(currentStream::close);
            }
            currentStream = new CloseTrackingInputStream(new ByteArrayInputStream(content));
            createdStreams.add(currentStream);
            return currentStream;
        }

        List<CloseTrackingInputStream> getCreatedStreams() {
            return createdStreams;
        }
    }

    private static class CloseTrackingInputStream extends FilterInputStream {
        private boolean isClosed = false;

        CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            isClosed = true;
        }

        boolean isClosed() {
            return isClosed;
        }
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private boolean isMd5Enabled;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            isMd5Enabled = executionAttributes.getAttribute(CHECKSUM) != null;
        }

        public boolean isMd5Enabled() {
            return isMd5Enabled;
        }
    }
}
