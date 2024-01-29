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

package software.amazon.awssdk.core.internal.progress.listener;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.testutils.RandomTempFile;

public class ProgressUpdaterTest {
    private CaptureProgressListener captureProgressListener;

    private static final long BYTES_TRANSFERRED = 5L;

    private static File sourceFile;

    private static final long OBJ_SIZE = 16 * 1024 * 1024;

    @BeforeEach
    void initiate() {
        captureProgressListener = new CaptureProgressListener();
    }
    private static CompletableFuture<SdkResponse> completedSdkResponse(long millis) {
        return CompletableFuture.supplyAsync(() -> {
            quietSleep(millis);

            VoidSdkResponse.Builder builder = (VoidSdkResponse.Builder) new VoidSdkResponse.Builder().sdkHttpResponse(null);
            return new VoidSdkResponse(builder);
        });
    }

        private static Stream<Arguments> contentLength() {
        return Stream.of(
            Arguments.of(100L),
            Arguments.of(200L),
            Arguments.of(300L),
            Arguments.of(400L),
            Arguments.of(500L));
    }

    @Test
    public void test_requestPrepared_transferredBytes_equals_zero() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, null);
        progressUpdater.requestPrepared();

        assertEquals(0.0, progressUpdater.uploadProgress().progressSnapshot().transferredBytes(), 0.0);
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.AttemptFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.AttemptFailureResponseBytesReceived.class));
        Mockito.verify(mockListener, times(1)).requestPrepared(ArgumentMatchers.any(ProgressListener.Context.RequestPrepared.class));


    }

    @Test
    public void test_requestHeaderSent_transferredBytes_equals_zero() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, null);
        progressUpdater.requestHeaderSent();

        assertEquals(0.0, progressUpdater.uploadProgress().progressSnapshot().transferredBytes(), 0.0);
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.AttemptFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.AttemptFailureResponseBytesReceived.class));
        Mockito.verify(mockListener, times(1)).requestHeaderSent(ArgumentMatchers.any(ProgressListener.Context.RequestHeaderSent.class));

    }

    @Test
    public void test_requestBytesSent_transferredBytes() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, null);
        progressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED, progressUpdater.uploadProgress().progressSnapshot().transferredBytes(), 0.0);

        progressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED + BYTES_TRANSFERRED, progressUpdater.uploadProgress().progressSnapshot().transferredBytes(), 0.0);

        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.AttemptFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.AttemptFailureResponseBytesReceived.class));
        Mockito.verify(mockListener, times(2)).requestBytesSent(ArgumentMatchers.any(ProgressListener.Context.RequestBytesSent.class));

    }

    @ParameterizedTest
    @MethodSource("contentLength")
    public void test_ratioTransferred_upload_transferredBytes(long contentLength) {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, contentLength);
        progressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals((double) BYTES_TRANSFERRED / contentLength, progressUpdater.uploadProgress().progressSnapshot().ratioTransferred().getAsDouble(), 0.0);

    }

    @Test
    public void test_responseHeaderReceived_transferredBytes_equals_zero() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, null);
        progressUpdater.responseHeaderReceived();

        assertEquals(0.0, progressUpdater.downloadProgress().progressSnapshot().transferredBytes(), 0.0);
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.AttemptFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.AttemptFailureResponseBytesReceived.class));
        Mockito.verify(mockListener, times(1)).responseHeaderReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseHeaderReceived.class));

    }

    @Test
    public void test_responseBytesReceived_transferredBytes() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, null);
        progressUpdater.incrementBytesReceived(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED, progressUpdater.downloadProgress().progressSnapshot().transferredBytes(), 0.0);

        progressUpdater.incrementBytesReceived(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED + BYTES_TRANSFERRED, progressUpdater.downloadProgress().progressSnapshot().transferredBytes(), 0.0);

        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.AttemptFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.AttemptFailureResponseBytesReceived.class));
        Mockito.verify(mockListener, times(2)).responseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseBytesReceived.class));

    }

    @ParameterizedTest(name = "{index} - {1}, total bytes = {0}")
    @MethodSource("contentLength")
    void registerCompletion_differentTransferredByteRatios_alwaysCompletesOnce(Long givenContentLength)
        throws Exception {
        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        sourceFile = new RandomTempFile(OBJ_SIZE);
        AsyncRequestBody requestBody = AsyncRequestBody.fromFile(sourceFile);
        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, givenContentLength);
        AsyncRequestBody asyncRequestBody = progressUpdater.wrapUploadRequestBody(requestBody);

        CompletableFuture<SdkResponse> completionFuture = completedSdkResponse(10);
        progressUpdater.registerCompletion(completionFuture);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);
        asyncRequestBody.subscribe(subscriber);

        captureProgressListener.getCompletionFuture().get(5, TimeUnit.SECONDS);

        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(1)).executionSuccess(ArgumentMatchers.any(ProgressListener.Context.ExecutionSuccess.class));
    }

    @Test
    void executionFailure_when_SubscriptionErrors() throws Exception {
        Long contentLength = 51L;
        String inputString = RandomStringUtils.randomAlphanumeric(contentLength.intValue());

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);
        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();
        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        sourceFile = new RandomTempFile(OBJ_SIZE);
        AsyncRequestBody requestFileBody = AsyncRequestBody.fromInputStream(
            new ExceptionThrowingByteArrayInputStream(inputString.getBytes(), 3), contentLength,
            Executors.newSingleThreadExecutor());

        ProgressUpdater progressUpdater = new ProgressUpdater(sdkRequest, contentLength);
        AsyncRequestBody asyncRequestBody = progressUpdater.wrapUploadRequestBody(requestFileBody);

        CompletableFuture<SdkResponse> completionFuture = completedSdkResponse(10);
        progressUpdater.registerCompletion(completionFuture);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);
        asyncRequestBody.subscribe(subscriber);

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(
            () -> captureProgressListener.getCompletionFuture().get(5, TimeUnit.SECONDS));

        Mockito.verify(mockListener, times(1)).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).executionSuccess(ArgumentMatchers.any(ProgressListener.Context.ExecutionSuccess.class));
    }

    private static class ExceptionThrowingByteArrayInputStream extends ByteArrayInputStream {
        private final int exceptionPosition;

        public ExceptionThrowingByteArrayInputStream(byte[] buf, int exceptionPosition) {
            super(buf);
            this.exceptionPosition = exceptionPosition;
        }

        @Override
        public int read() {
            return (exceptionPosition == pos + 1) ? exceptionThrowingRead() : super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return (exceptionPosition >= pos && exceptionPosition < (pos + len)) ?
                   exceptionThrowingReadByteArr(b, off, len) : super.read(b, off, len);
        }

        private int exceptionThrowingRead() {
            throw new RuntimeException("Exception occurred at position " + (pos + 1));
        }

        private int exceptionThrowingReadByteArr(byte[] b, int off, int len) {
            throw new RuntimeException("Exception occurred in read(byte[]) at position " + exceptionPosition);
        }
    }

    private static void quietSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new RuntimeException(e);
        }
    }
}
