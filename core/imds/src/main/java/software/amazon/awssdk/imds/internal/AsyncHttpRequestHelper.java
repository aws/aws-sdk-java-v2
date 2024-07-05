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

import static software.amazon.awssdk.imds.internal.BaseEc2MetadataClient.uncheckedInputStreamToUtf8;
import static software.amazon.awssdk.imds.internal.RequestMarshaller.EC2_METADATA_TOKEN_TTL_HEADER;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
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
        return sendAsync(httpClient, baseRequest, AsyncHttpRequestHelper::handleResponse, parentFuture);
    }

    public static CompletableFuture<Token> sendAsyncTokenRequest(SdkAsyncHttpClient httpClient,
                                                                 SdkHttpFullRequest baseRequest) {
        return sendAsync(httpClient, baseRequest, AsyncHttpRequestHelper::handleTokenResponse, null);
    }

    private static <T> CompletableFuture<T> sendAsync(SdkAsyncHttpClient client,
                                                      SdkHttpFullRequest request,
                                                      HttpResponseHandler<T> handler,
                                                      CompletableFuture<?> parentFuture) {
        SdkHttpContentPublisher requestContentPublisher = new SimpleHttpContentPublisher(request);
        TransformingAsyncResponseHandler<T> responseHandler =
            new AsyncResponseHandler<>(handler, Function.identity(), new ExecutionAttributes());
        CompletableFuture<T> responseHandlerFuture = responseHandler.prepare();
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

    private static String handleResponse(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) {
        HttpStatusFamily statusCode = HttpStatusFamily.of(response.statusCode());
        AbortableInputStream inputStream =
            response.content().orElseThrow(() -> SdkClientException.create("Unexpected error: empty response content"));
        String responseContent = uncheckedInputStreamToUtf8(inputStream);

        // non-retryable error
        if (statusCode.isOneOf(HttpStatusFamily.CLIENT_ERROR)) {
            throw SdkClientException.builder().message(responseContent).build();
        }

        // retryable error
        if (statusCode.isOneOf(HttpStatusFamily.SERVER_ERROR)) {
            throw RetryableException.create(responseContent);
        }
        return responseContent;
    }

    private static Token handleTokenResponse(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) {
        String tokenValue = handleResponse(response, executionAttributes);
        Optional<String> ttl = response.firstMatchingHeader(EC2_METADATA_TOKEN_TTL_HEADER);

        if (!ttl.isPresent()) {
            throw SdkClientException.create(EC2_METADATA_TOKEN_TTL_HEADER + " header not found in token response");
        }
        try {
            Duration ttlDuration = Duration.ofSeconds(Long.parseLong(ttl.get()));
            return new Token(tokenValue, ttlDuration);
        } catch (NumberFormatException nfe) {
            throw SdkClientException.create(
                "Invalid token format received from IMDS server. Token received:  " + tokenValue, nfe);
        }
    }
}
