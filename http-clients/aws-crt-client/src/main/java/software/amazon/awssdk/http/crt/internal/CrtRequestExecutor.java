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

package software.amazon.awssdk.http.crt.internal;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.response.CrtResponseAdapter;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class CrtRequestExecutor {
    private static final Logger log = Logger.loggerFor(CrtRequestExecutor.class);

    public CompletableFuture<Void> execute(CrtRequestContext executionContext) {
        CompletableFuture<Void> requestFuture = createExecutionFuture(executionContext.sdkRequest());

        // When a Connection is ready from the Connection Pool, schedule the Request on the connection
        CompletableFuture<HttpClientConnection> httpClientConnectionCompletableFuture =
            executionContext.crtConnPool().acquireConnection();

        httpClientConnectionCompletableFuture.whenComplete((crtConn, throwable) -> {
            AsyncExecuteRequest asyncRequest = executionContext.sdkRequest();
            // If we didn't get a connection for some reason, fail the request
            if (throwable != null) {
                reportFailure(new IOException("An exception occurred when acquiring a connection", throwable),
                              requestFuture,
                              asyncRequest.responseHandler());
                return;
            }

            HttpRequest crtRequest = CrtRequestAdapter.toCrtRequest(executionContext);
            HttpStreamResponseHandler crtResponseHandler =
                CrtResponseAdapter.toCrtResponseHandler(crtConn, requestFuture, executionContext);

            // Submit the request on the connection
            try {
                crtConn.makeRequest(crtRequest, crtResponseHandler).activate();
            } catch (IllegalStateException | CrtRuntimeException e) {
                reportFailure(new IOException("An exception occurred when making the request", e),
                              requestFuture,
                              asyncRequest.responseHandler());
            }
        });

        return requestFuture;
    }

    /**
     * Create the execution future and set up the cancellation logic.
     * @return The created execution future.
     */
    private CompletableFuture<Void> createExecutionFuture(AsyncExecuteRequest request) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        future.whenComplete((r, t) -> {
            if (t == null) {
                return;
            }

            // TODO: Aborting request once it's supported in CRT
            if (future.isCancelled()) {
                request.responseHandler().onError(new SdkCancellationException("The request was cancelled"));
            }
        });

        return future;
    }

    /**
     * Notify the provided response handler and future of the failure.
     */
    private void reportFailure(Throwable cause,
                               CompletableFuture<Void> executeFuture,
                               SdkAsyncHttpResponseHandler responseHandler) {
        try {
            responseHandler.onError(cause);
        } catch (Exception e) {
            log.error(() -> "SdkAsyncHttpResponseHandler " + responseHandler + " threw an exception in onError. It will be "
                            + "ignored.", e);
        }
        executeFuture.completeExceptionally(cause);
    }
}
