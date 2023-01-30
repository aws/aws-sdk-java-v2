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

package software.amazon.awssdk.core.internal.http.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;

class CombinedResponseAsyncHttpResponseHandlerTest {

    private CombinedResponseAsyncHttpResponseHandler<Void> responseHandler;
    private TransformingAsyncResponseHandler<Void> successResponseHandler;
    private TransformingAsyncResponseHandler<SdkClientException> errorResponseHandler;

    @BeforeEach
    public void setup() {
        successResponseHandler = Mockito.mock(TransformingAsyncResponseHandler.class);
        errorResponseHandler = Mockito.mock(TransformingAsyncResponseHandler.class);
        responseHandler = new CombinedResponseAsyncHttpResponseHandler<>(successResponseHandler, errorResponseHandler);
    }

    @Test
    void onStream_invokedWithoutPrepare_shouldThrowException() {
        assertThatThrownBy(() ->
                               responseHandler.onStream(publisher()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("onStream() invoked");

    }

    @Test
    void onHeaders_invokedWithoutPrepare_shouldThrowException() {
        assertThatThrownBy(() ->
                               responseHandler.onHeaders(SdkHttpFullResponse.builder().build()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("onHeaders() invoked");

    }

    @Test
    void onStream_invokedWithoutOnHeaders_shouldThrowException() {
        when(successResponseHandler.prepare()).thenReturn(CompletableFuture.completedFuture(null));

        responseHandler.prepare();
        assertThatThrownBy(() ->
                               responseHandler.onStream(publisher()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("headersFuture is still not completed when onStream()");

    }

    @Test
    void onStream_HeadersFutureCompleteSuccessfully_shouldNotThrowException() {
        when(successResponseHandler.prepare()).thenReturn(CompletableFuture.completedFuture(null));

        responseHandler.prepare();
        responseHandler.onError(new RuntimeException("error"));
        Flowable<ByteBuffer> publisher = publisher();
        responseHandler.onStream(publisher);
        verify(successResponseHandler, times(0)).onStream(publisher);
        verify(errorResponseHandler, times(0)).onStream(publisher);
    }

    @Test
    void successResponse_shouldCompleteHeaderFuture() {
        when(successResponseHandler.prepare()).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Response<Void>> future = responseHandler.prepare();
        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                       .statusCode(200)
                                                       .build();
        Flowable<ByteBuffer> publisher = publisher();
        responseHandler.onHeaders(sdkHttpFullResponse);
        responseHandler.onStream(publisher);
        verify(successResponseHandler).prepare();
        verify(successResponseHandler).onStream(publisher);
        assertThat(future).isDone();
        assertThat(future.join().httpResponse()).isEqualTo(sdkHttpFullResponse);
    }

    @Test
    void errorResponse_shouldCompleteHeaderFuture() {
        when(errorResponseHandler.prepare()).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Response<Void>> future = responseHandler.prepare();
        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(400)
                                                                     .build();
        Flowable<ByteBuffer> publisher = publisher();
        responseHandler.onHeaders(sdkHttpFullResponse);
        responseHandler.onStream(publisher);
        verify(errorResponseHandler).prepare();
        verify(errorResponseHandler).onStream(publisher);
        assertThat(future).isDone();
        assertThat(future.join().httpResponse()).isEqualTo(sdkHttpFullResponse);
    }

    private static Flowable<ByteBuffer> publisher() {
        return Flowable.just(ByteBuffer.wrap("string".getBytes(StandardCharsets.UTF_8)));
    }

}
