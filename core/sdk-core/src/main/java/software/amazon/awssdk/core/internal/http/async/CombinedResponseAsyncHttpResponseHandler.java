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

import static software.amazon.awssdk.core.SdkStandardLogger.logRequestId;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Detects whether the response succeeded or failed by just checking the HTTP status and delegates to appropriate
 * async response handler. Can be used with streaming or non-streaming requests.
 */
@SdkInternalApi
public final class CombinedResponseAsyncHttpResponseHandler<OutputT>
    implements TransformingAsyncResponseHandler<Response<OutputT>> {

    private final TransformingAsyncResponseHandler<OutputT> successResponseHandler;
    private final TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler;
    private CompletableFuture<SdkHttpResponse> headersFuture;

    public CombinedResponseAsyncHttpResponseHandler(
        TransformingAsyncResponseHandler<OutputT> successResponseHandler,
        TransformingAsyncResponseHandler<? extends SdkException> errorResponseHandler) {

        this.successResponseHandler = successResponseHandler;
        this.errorResponseHandler = errorResponseHandler;
    }

    @Override
    public void onHeaders(SdkHttpResponse response) {
        Validate.isTrue(headersFuture != null, "onHeaders() invoked without prepare().");
        headersFuture.complete(response);
        logRequestId(response);

        if (response.isSuccessful()) {
            successResponseHandler.onHeaders(response);
        } else {
            errorResponseHandler.onHeaders(response);
        }
    }

    @Override
    public void onError(Throwable error) {
        if (headersFuture != null) {        // Failure in marshalling calls this before prepare() so value is null
            headersFuture.completeExceptionally(error);
        }

        successResponseHandler.onError(error);
        errorResponseHandler.onError(error);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        Validate.isTrue(headersFuture != null, "onStream() invoked without prepare().");
        Validate.isTrue(headersFuture.isDone(), "headersFuture is still not completed when onStream() is "
                                                + "invoked.");

        if (headersFuture.isCompletedExceptionally()) {
            return;
        }

        SdkHttpResponse sdkHttpResponse = headersFuture.join();
        if (sdkHttpResponse.isSuccessful()) {
            successResponseHandler.onStream(publisher);
        } else {
            errorResponseHandler.onStream(publisher);
        }
    }

    @Override
    public CompletableFuture<Response<OutputT>> prepare() {
        headersFuture = new CompletableFuture<>();
        CompletableFuture<OutputT> preparedTransformFuture = successResponseHandler.prepare();

        CompletableFuture<? extends SdkException> preparedErrorTransformFuture = errorResponseHandler == null ? null :
            errorResponseHandler.prepare();

        return headersFuture.thenCompose(headers -> {
            SdkHttpFullResponse sdkHttpFullResponse = toFullResponse(headers);
            if (headers.isSuccessful()) {
                return preparedTransformFuture.thenApply(
                    r -> Response.<OutputT>builder().response(r)
                                                    .httpResponse(sdkHttpFullResponse)
                                                    .isSuccess(true)
                                                    .build());
            }

            if (preparedErrorTransformFuture != null) {
                return preparedErrorTransformFuture.thenApply(
                    e -> Response.<OutputT>builder().exception(e)
                                                    .httpResponse(sdkHttpFullResponse)
                                                    .isSuccess(false)
                                                    .build());
            }
            return CompletableFuture.completedFuture(
                Response.<OutputT>builder().httpResponse(sdkHttpFullResponse)
                                           .isSuccess(false)
                                           .build());
        });
    }

    private static SdkHttpFullResponse toFullResponse(SdkHttpResponse response) {
        SdkHttpFullResponse.Builder builder = SdkHttpFullResponse.builder()
                                                                 .statusCode(response.statusCode());
        response.forEachHeader(builder::putHeader);
        response.statusText().ifPresent(builder::statusText);
        return builder.build();
    }
}