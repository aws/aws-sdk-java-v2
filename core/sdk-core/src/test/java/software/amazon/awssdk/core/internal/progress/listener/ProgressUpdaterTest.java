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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

class ProgressUpdaterTest {
    private CaptureProgressListener captureProgressListener;
    private static final long BYTES_TRANSFERRED = 5L;
    private static final Throwable attemptFailure = new Throwable("AttemptFailureException");
    private static final Throwable executionFailure = new Throwable("ExecutionFailureException");
    private static final Throwable attemptFailureResponseBytesReceived
        = new Throwable("AttemptFailureResponseBytesReceivedException");

    @BeforeEach
    void initiate() {
        captureProgressListener = new CaptureProgressListener();
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
    void requestPrepared_transferredBytes_equals_zero() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.requestPrepared(createHttpRequest());

        assertEquals(0.0, defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);
        assertTrue(captureProgressListener.requestPrepared());
        assertFalse(captureProgressListener.requestHeaderSent());
        assertFalse(captureProgressListener.responseHeaderReceived());
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(1)).requestPrepared(ArgumentMatchers.any(ProgressListener.Context.RequestPrepared.class));


    }

    @Test
    void requestHeaderSent_transferredBytes_equals_zero() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.requestHeaderSent();

        assertEquals(0.0, defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);
        assertFalse(captureProgressListener.requestPrepared());
        assertTrue(captureProgressListener.requestHeaderSent());
        assertFalse(captureProgressListener.responseHeaderReceived());
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(1)).requestHeaderSent(ArgumentMatchers.any(ProgressListener.Context.RequestHeaderSent.class));

    }

    @Test
    void requestBytesSent_transferredBytes() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED, defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);

        defaultProgressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED + BYTES_TRANSFERRED,
                     defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);

        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(2)).requestBytesSent(ArgumentMatchers.any(ProgressListener.Context.RequestBytesSent.class));

    }

    @Test
    void validate_resetBytesSent() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED, defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);

        defaultProgressUpdater.resetBytesSent();
        assertEquals(0, defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);

    }

    @Test
    void validate_resetBytesReceived() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.incrementBytesReceived(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED, defaultProgressUpdater.responseBodyProgress().progressSnapshot().transferredBytes(), 0.0);

        defaultProgressUpdater.resetBytesReceived();
        assertEquals(0, defaultProgressUpdater.responseBodyProgress().progressSnapshot().transferredBytes(), 0.0);

    }

    @ParameterizedTest
    @MethodSource("contentLength")
    void ratioTransferred_upload_transferredBytes(long contentLength) {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.updateRequestContentLength(contentLength);
        defaultProgressUpdater.incrementBytesSent(BYTES_TRANSFERRED);
        assertEquals((double) BYTES_TRANSFERRED / contentLength,
                     defaultProgressUpdater.requestBodyProgress().progressSnapshot().ratioTransferred().getAsDouble(), 0.0);

    }

    @ParameterizedTest
    @MethodSource("contentLength")
    void ratioTransferred_download_transferredBytes(long contentLength) {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.updateResponseContentLength(contentLength);
        defaultProgressUpdater.incrementBytesReceived(BYTES_TRANSFERRED);
        assertEquals((double) BYTES_TRANSFERRED / contentLength,
                     defaultProgressUpdater.responseBodyProgress().progressSnapshot().ratioTransferred().getAsDouble(), 0.0);

    }

    @Test
    void responseHeaderReceived_transferredBytes_equals_zero() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.responseHeaderReceived();

        assertEquals(0.0, defaultProgressUpdater.requestBodyProgress().progressSnapshot().transferredBytes(), 0.0);
        assertFalse(captureProgressListener.requestPrepared());
        assertFalse(captureProgressListener.requestHeaderSent());
        assertTrue(captureProgressListener.responseHeaderReceived());
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(1)).responseHeaderReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseHeaderReceived.class));

    }

    @Test
    void executionSuccess_transferredBytes_valid() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.incrementBytesReceived(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED, defaultProgressUpdater.responseBodyProgress().progressSnapshot().transferredBytes(), 0.0);

        defaultProgressUpdater.incrementBytesReceived(BYTES_TRANSFERRED);
        assertEquals(BYTES_TRANSFERRED + BYTES_TRANSFERRED,
                     defaultProgressUpdater.responseBodyProgress().progressSnapshot().transferredBytes(), 0.0);

        defaultProgressUpdater.executionSuccess(VoidSdkResponse.builder().sdkHttpResponse(null).build());
        Mockito.verify(mockListener, never()).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, never()).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(2)).responseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseBytesReceived.class));
        Mockito.verify(mockListener, times(1)).executionSuccess(ArgumentMatchers.any(ProgressListener.Context.ExecutionSuccess.class));
    }

    @Test
    void attemptFailureResponseBytesReceived() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.requestPrepared(createHttpRequest());
        defaultProgressUpdater.responseHeaderReceived();
        defaultProgressUpdater.attemptFailureResponseBytesReceived(attemptFailureResponseBytesReceived);

        Mockito.verify(mockListener, times(1)).requestPrepared(ArgumentMatchers.any(ProgressListener.Context.RequestPrepared.class));
        Mockito.verify(mockListener, times(1)).responseHeaderReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseHeaderReceived.class));
        Mockito.verify(mockListener, times(1)).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(0)).responseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseBytesReceived.class));
        Mockito.verify(mockListener, times(0)).executionSuccess(ArgumentMatchers.any(ProgressListener.Context.ExecutionSuccess.class));

        Assertions.assertEquals(captureProgressListener.exceptionCaught().getMessage(), attemptFailureResponseBytesReceived.getMessage());
    }

    @Test
    void attemptFailure() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.requestPrepared(createHttpRequest());
        defaultProgressUpdater.attemptFailure(attemptFailure);

        Mockito.verify(mockListener, times(1)).requestPrepared(ArgumentMatchers.any(ProgressListener.Context.RequestPrepared.class));
        Mockito.verify(mockListener, times(0)).responseHeaderReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseHeaderReceived.class));
        Mockito.verify(mockListener, times(0)).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(0)).responseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseBytesReceived.class));
        Mockito.verify(mockListener, times(0)).executionSuccess(ArgumentMatchers.any(ProgressListener.Context.ExecutionSuccess.class));
        Mockito.verify(mockListener, times(1)).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(0)).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));

        Assertions.assertEquals(captureProgressListener.exceptionCaught().getMessage(), attemptFailure.getMessage());
    }

    @Test
    void executionFailure() {

        CaptureProgressListener mockListener = Mockito.mock(CaptureProgressListener.class);

        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        builder.progressListeners(Arrays.asList(mockListener, captureProgressListener));

        SdkRequestOverrideConfiguration overrideConfig = builder.build();

        SdkRequest sdkRequest = NoopTestRequest.builder()
                                               .overrideConfiguration(overrideConfig)
                                               .build();

        DefaultProgressUpdater defaultProgressUpdater = new DefaultProgressUpdater(sdkRequest, null);
        defaultProgressUpdater.requestPrepared(createHttpRequest());
        defaultProgressUpdater.executionFailure(executionFailure);


        Mockito.verify(mockListener, times(1)).requestPrepared(ArgumentMatchers.any(ProgressListener.Context.RequestPrepared.class));
        Mockito.verify(mockListener, times(0)).responseHeaderReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseHeaderReceived.class));
        Mockito.verify(mockListener, times(0)).attemptFailureResponseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(0)).responseBytesReceived(ArgumentMatchers.any(ProgressListener.Context.ResponseBytesReceived.class));
        Mockito.verify(mockListener, times(0)).executionSuccess(ArgumentMatchers.any(ProgressListener.Context.ExecutionSuccess.class));
        Mockito.verify(mockListener, times(0)).attemptFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));
        Mockito.verify(mockListener, times(1)).executionFailure(ArgumentMatchers.any(ProgressListener.Context.ExecutionFailure.class));

        Assertions.assertEquals(captureProgressListener.exceptionCaught().getMessage(), executionFailure.getMessage());
    }

    private SdkHttpFullRequest createHttpRequest() {
        return SdkHttpFullRequest.builder().uri(URI.create("https://endpoint.host"))
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }
}
