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

package software.amazon.awssdk.imds.internal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
final class AsyncHttpRequestHelper {

    private AsyncHttpRequestHelper() {
        // static utility class
    }

    public static CompletableFuture<String> sendAsyncMetadataRequest(SdkAsyncHttpClient httpClient,
                                                                     SdkHttpFullRequest baseRequest,
                                                                     CompletableFuture<?> parentFuture) {
        StringResponseHandler stringResponseHandler = new StringResponseHandler();
        return sendAsync(httpClient, baseRequest, stringResponseHandler, stringResponseHandler::setFuture, parentFuture);
    }

    public static CompletableFuture<Token> sendAsyncTokenRequest(SdkAsyncHttpClient httpClient,
                                                                 SdkHttpFullRequest baseRequest) {
        TokenResponseHandler tokenResponseHandler = new TokenResponseHandler();
        return sendAsync(httpClient, baseRequest, tokenResponseHandler, tokenResponseHandler::setFuture, null);
    }

    static <T> CompletableFuture<T> sendAsync(SdkAsyncHttpClient client,
                                              SdkHttpFullRequest request,
                                              HttpResponseHandler<T> handler,
                                              Consumer<CompletableFuture<T>> withFuture,
                                              CompletableFuture<?> parentFuture) {
        SdkHttpContentPublisher requestContentPublisher = new SimpleHttpContentPublisher(request);
        TransformingAsyncResponseHandler<T> responseHandler = new AsyncResponseHandler<>(handler,
                                                                                             Function.identity(),
                                                                                             new ExecutionAttributes());
        CompletableFuture<T> responseHandlerFuture = responseHandler.prepare();
        withFuture.accept(responseHandlerFuture);
        AsyncExecuteRequest metadataRequest = AsyncExecuteRequest.builder()
                                                                 .request(request)
                                                                 .requestContentPublisher(requestContentPublisher)
                                                                 .responseHandler(responseHandler)
                                                                 .build();
        CompletableFuture<Void> executeFuture = client.execute(metadataRequest);
        if (parentFuture != null) {
            CompletableFutureUtils.forwardExceptionTo(parentFuture, executeFuture);
            CompletableFutureUtils.forwardExceptionTo(parentFuture, responseHandlerFuture);
        }
        return responseHandlerFuture;

    }

}
