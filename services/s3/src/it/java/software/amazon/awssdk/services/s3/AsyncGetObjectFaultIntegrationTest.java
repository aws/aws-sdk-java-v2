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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class AsyncGetObjectFaultIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(AsyncGetObjectFaultIntegrationTest.class);

    private static final String KEY = "some-key";

    private static S3AsyncClient s3ClientWithTimeout;

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), RequestBody.fromString("some contents"));
        s3ClientWithTimeout = s3AsyncClientBuilder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                              .apiCallTimeout(Duration.ofSeconds(1))
                                                              .build())
            .build();
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void slowTransformer_shouldThrowApiCallTimeoutException() {
        SlowResponseTransformer<GetObjectResponse> handler =
            new SlowResponseTransformer<>();
        assertThatThrownBy(() -> s3ClientWithTimeout.getObject(getObjectRequest(), handler).join())
                .hasCauseInstanceOf(ApiCallTimeoutException.class);
        assertThat(handler.currentCallCount()).isEqualTo(1);
        assertThat(handler.exceptionOccurred).isEqualTo(true);
    }

    private GetObjectRequest getObjectRequest() {
        return GetObjectRequest.builder()
                               .bucket(BUCKET)
                               .key(KEY)
                               .build();
    }

    /**
     * Wrapper around a {@link AsyncResponseTransformer} that counts how many times it's been invoked.
     */
    private static class SlowResponseTransformer<ResponseT>
        implements AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> {

        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> delegate;
        private boolean exceptionOccurred = false;

        private SlowResponseTransformer() {
            this.delegate = AsyncResponseTransformer.toBytes();
        }

        public int currentCallCount() {
            return callCount.get();
        }


        @Override
        public CompletableFuture<ResponseBytes<ResponseT>> prepare() {
            return delegate.prepare()
                    .thenApply(r -> {
                        try {
                            Thread.sleep(2_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();;
                        }
                        return r;
                    });
        }

        @Override
        public void onResponse(ResponseT response) {
            callCount.incrementAndGet();
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            delegate.exceptionOccurred(throwable);
            exceptionOccurred = true;
        }
    }
}
