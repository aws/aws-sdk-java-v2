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


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public class S3CrtResponseHandlerAdapterTest {
    private S3CrtResponseHandlerAdapter responseHandlerAdapter;

    @Mock
    private SdkAsyncHttpResponseHandler sdkResponseHandler;

    @Mock
    private S3CrtDataPublisher crtDataPublisher;
    private CompletableFuture<Void> future;

    @Before
    public void setup() {
        future = new CompletableFuture<>();
        responseHandlerAdapter = new S3CrtResponseHandlerAdapter(future,
                                                                 sdkResponseHandler,
                                                                 crtDataPublisher);
    }

    @Test
    public void successfulResponse_shouldCompleteFutureSuccessfully() {
        HttpHeader[] httpHeaders = new HttpHeader[2];
        httpHeaders[0] = new HttpHeader("foo", "1");
        httpHeaders[1] = new HttpHeader("bar", "2");

        int statusCode = 200;
        responseHandlerAdapter.onResponseHeaders(statusCode, httpHeaders);

        ArgumentCaptor<SdkHttpResponse> argumentCaptor = ArgumentCaptor.forClass(SdkHttpResponse.class);
        verify(sdkResponseHandler).onHeaders(argumentCaptor.capture());

        SdkHttpResponse actualSdkHttpResponse = argumentCaptor.getValue();
        assertThat(actualSdkHttpResponse.statusCode()).isEqualTo(statusCode);
        assertThat(actualSdkHttpResponse.firstMatchingHeader("foo")).contains("1");
        assertThat(actualSdkHttpResponse.firstMatchingHeader("bar")).contains("2");

        verify(sdkResponseHandler).onStream(crtDataPublisher);

        responseHandlerAdapter.onFinished(0, 0, null);
        assertThat(future).isCompleted();
    }

    @Test
    public void errorResponse_shouldCompleteFutureSuccessfully() {
        int statusCode = 400;
        responseHandlerAdapter.onResponseHeaders(statusCode, new HttpHeader[0]);

        ArgumentCaptor<SdkHttpResponse> argumentCaptor = ArgumentCaptor.forClass(SdkHttpResponse.class);
        verify(sdkResponseHandler).onHeaders(argumentCaptor.capture());

        SdkHttpResponse actualSdkHttpResponse = argumentCaptor.getValue();
        assertThat(actualSdkHttpResponse.statusCode()).isEqualTo(400);
        assertThat(actualSdkHttpResponse.headers()).isEmpty();

        verify(sdkResponseHandler).onStream(crtDataPublisher);

        byte[] errorPayload = "errorResponse".getBytes(StandardCharsets.UTF_8);
        responseHandlerAdapter.onFinished(1, statusCode, errorPayload);

        ArgumentCaptor<ByteBuffer> byteBufferArgumentCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(crtDataPublisher).deliverData(byteBufferArgumentCaptor.capture());

        ByteBuffer actualByteBuffer = byteBufferArgumentCaptor.getValue();

        assertThat(actualByteBuffer).isEqualTo(ByteBuffer.wrap(errorPayload));

        assertThat(future).isCompleted();
    }

    @Test
    public void requestFailed_shouldCompleteFutureExceptionally() {

        responseHandlerAdapter.onFinished(1, 0, null);

        ArgumentCaptor<Exception> exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(crtDataPublisher).notifyError(exceptionArgumentCaptor.capture());
        verify(sdkResponseHandler).onError(exceptionArgumentCaptor.capture());

        Exception actualException = exceptionArgumentCaptor.getValue();
        assertThat(actualException).isInstanceOf(SdkClientException.class);
        assertThat(future).isCompletedExceptionally();
    }
}
