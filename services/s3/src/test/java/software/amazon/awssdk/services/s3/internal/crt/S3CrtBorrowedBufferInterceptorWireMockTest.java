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

package software.amazon.awssdk.services.s3.internal.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/** Verifies that response-content interceptors do not receive borrowed object-body data. */
@WireMockTest
@Timeout(20)
class S3CrtBorrowedBufferInterceptorWireMockTest {
    private static final int PART_SIZE = 256 * 1024;
    private static final byte[] CONTENT = payload(PART_SIZE);
    private static final String E_TAG = "\"interceptor-etag\"";

    @Test
    void borrowedDownload_shouldInvokeContentInterceptorWithNoBody(WireMockRuntimeInfo wireMock) throws Exception {
        stubSuccessfulObject();
        RecordingInterceptor interceptor = new RecordingInterceptor();

        try (S3AsyncClient client = newClient(wireMock, interceptor);
             ResponseInputStream<GetObjectResponse> stream =
                 client.getObject(r -> r.bucket("bucket").key("key"),
                                  S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers())
                       .get(10, TimeUnit.SECONDS)) {
            assertThat(readAll(stream)).containsExactly(CONTENT);
            assertThat(stream.read()).isEqualTo(-1);
        }

        // The hook receives a publisher and subscribes to the wrapper, but borrowed bytes bypass it.
        assertThat(interceptor.modifyAsyncHttpResponseContentCalls).isEqualTo(1);
        assertThat(interceptor.responsePublisherPresentCalls).isEqualTo(1);
        assertThat(interceptor.responsePublisherSubscribedCalls).isEqualTo(1);
        assertThat(interceptor.bodyBytesSeen).isZero();

        assertThat(interceptor.modifyResponseCalls).isPositive();
        assertThat(interceptor.afterExecutionCalls).isEqualTo(1);
        assertThat(interceptor.onExecutionFailureCalls).isZero();
    }

    @Test
    void ordinaryDownloadOnPoolEnabledClient_shouldStillInvokeResponseContentInterceptor(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubSuccessfulObject();
        RecordingInterceptor interceptor = new RecordingInterceptor();

        // Same client configuration, same pool: only the transformer differs. This is what isolates the difference to
        // borrowed delivery rather than to enabling the pool.
        try (S3AsyncClient client = newClient(wireMock, interceptor)) {
            byte[] bytes = client.getObject(r -> r.bucket("bucket").key("key"),
                                            AsyncResponseTransformer.toBytes())
                                 .get(10, TimeUnit.SECONDS)
                                 .asByteArray();
            assertThat(bytes).containsExactly(CONTENT);
        }

        assertThat(interceptor.modifyAsyncHttpResponseContentCalls).isPositive();
        assertThat(interceptor.responsePublisherPresentCalls).isPositive();
        assertThat(interceptor.bodyBytesSeen).isEqualTo(CONTENT.length);
        assertThat(interceptor.afterExecutionCalls).isEqualTo(1);
    }

    @Test
    void borrowedDownloadThatFails_shouldStillInvokeOnExecutionFailure(WireMockRuntimeInfo wireMock) {
        byte[] errorBody = ("<Error><Code>NoSuchKey</Code><Message>missing</Message>"
                            + "<RequestId>request-id</RequestId></Error>").getBytes(StandardCharsets.UTF_8);
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(404)
                                                    .withHeader("x-amz-request-id", "request-id")
                                                    .withHeader("Content-Length", "0")));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(404)
                                                   .withHeader("Content-Type", "application/xml")
                                                   .withHeader("Content-Length", Integer.toString(errorBody.length))
                                                   .withHeader("x-amz-request-id", "request-id")
                                                   .withBody(errorBody)));
        RecordingInterceptor interceptor = new RecordingInterceptor();

        try (S3AsyncClient client = newClient(wireMock, interceptor)) {
            assertThatThrownBy(() -> client.getObject(
                r -> r.bucket("bucket").key("key"),
                S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers()).get(10, TimeUnit.SECONDS))
                .isNotNull();
        }

        assertThat(interceptor.onExecutionFailureCalls).isEqualTo(1);
        // The error document is not a borrowed body: it comes back through the ordinary response pipeline so that it
        // can be unmarshalled into an S3Exception. So a content interceptor does see the error payload, and sees only
        // that. This asymmetry with the success path is deliberate and worth pinning.
        assertThat(interceptor.modifyAsyncHttpResponseContentCalls).isEqualTo(1);
        assertThat(interceptor.bodyBytesSeen).isEqualTo(errorBody.length);
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock, ExecutionInterceptor interceptor) {
        DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder builder =
            (DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder) S3AsyncClient.crtBuilder()
                         .region(Region.US_EAST_1)
                         .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                         .credentialsProvider(StaticCredentialsProvider.create(
                             AwsBasicCredentials.create("key", "secret")))
                         .minimumPartSizeInBytes((long) PART_SIZE)
                         .initialReadBufferSizeInBytes((long) PART_SIZE)
                         .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                         .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(PART_SIZE));
        return builder.addExecutionInterceptor(interceptor).build();
    }

    private static void stubSuccessfulObject() {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Content-Length", Integer.toString(CONTENT.length))
                                                    .withHeader("ETag", E_TAG)));
        stubFor(get(anyUrl()).withHeader("Range", equalTo("bytes=0-" + (CONTENT.length - 1)))
                             .willReturn(aResponse().withStatus(206)
                                                        .withHeader("Content-Length", Integer.toString(CONTENT.length))
                                                        .withHeader("Content-Range",
                                                                    "bytes 0-" + (CONTENT.length - 1)
                                                                    + "/" + CONTENT.length)
                                                        .withHeader("ETag", E_TAG)
                                                        .withBody(CONTENT)));
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            result.write(buffer, 0, read);
        }
        return result.toByteArray();
    }

    private static byte[] payload(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }

    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private final List<String> order = new CopyOnWriteArrayList<>();
        private volatile int modifyAsyncHttpResponseContentCalls;
        private volatile int responsePublisherPresentCalls;
        private volatile int responsePublisherSubscribedCalls;
        private volatile int modifyResponseCalls;
        private volatile int afterExecutionCalls;
        private volatile int onExecutionFailureCalls;
        private volatile long bodyBytesSeen;

        @Override
        public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                             ExecutionAttributes executionAttributes) {
            modifyAsyncHttpResponseContentCalls++;
            order.add("modifyAsyncHttpResponseContent");
            if (context.responsePublisher().isPresent()) {
                responsePublisherPresentCalls++;
            }
            return context.responsePublisher().map(publisher -> subscriber -> {
                responsePublisherSubscribedCalls++;
                publisher.subscribe(new CountingSubscriber(subscriber, read -> bodyBytesSeen += read));
            });
        }

        @Override
        public SdkResponse modifyResponse(Context.ModifyResponse context,
                                                                     ExecutionAttributes executionAttributes) {
            modifyResponseCalls++;
            order.add("modifyResponse");
            return context.response();
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            afterExecutionCalls++;
            order.add("afterExecution");
        }

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            onExecutionFailureCalls++;
            order.add("onExecutionFailure");
        }
    }

    private static final class CountingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> delegate;
        private final IntConsumer counter;

        private CountingSubscriber(Subscriber<? super ByteBuffer> delegate,
                                   IntConsumer counter) {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            counter.accept(buffer.remaining());
            delegate.onNext(buffer);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
