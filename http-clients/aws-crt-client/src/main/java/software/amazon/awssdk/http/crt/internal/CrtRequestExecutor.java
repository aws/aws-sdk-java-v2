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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.Http2Request;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class CrtRequestExecutor {
    private static final Logger log = Logger.loggerFor(CrtRequestExecutor.class);

    public CompletableFuture<Void> execute(CrtRequestContext executionContext, Protocol protocol) {
        CompletableFuture<Void> requestFuture = createExecutionFuture(executionContext.sdkRequest());

        AsyncExecuteRequest asyncRequest = executionContext.sdkRequest();

        AwsCrtAsyncHttpStreamAdapter crtToSdkAdapter =
            new AwsCrtAsyncHttpStreamAdapter(requestFuture, asyncRequest, executionContext.readBufferSize());

        HttpRequestBase crtRequest = toCrtRequest(asyncRequest, crtToSdkAdapter, protocol);

        // When a Connection is ready from the Connection Pool, schedule the Request on the connection
        CompletableFuture<HttpStreamBase> httpClientConnectionCompletableFuture =
            executionContext.crtConnPool().acquireStream(crtRequest, crtToSdkAdapter);

        httpClientConnectionCompletableFuture.whenComplete((crtConn, throwable) -> {
            log.error(() -> "An exception occurred when making the request", throwable);
            if (throwable != null) {
                handleFailure(new IOException("An exception occurred when acquiring the stream", throwable),
                              requestFuture,
                              asyncRequest.responseHandler());
            }
        });

        return requestFuture;
    }

    /**
     * Convenience method to create the execution future and set up the cancellation logic.
     *
     * @return The created execution future.
     */
    private CompletableFuture<Void> createExecutionFuture(AsyncExecuteRequest request) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        future.whenComplete((r, t) -> {
            if (t == null) {
                return;
            }
            //TODO: Aborting request once it's supported in CRT
            if (future.isCancelled()) {
                request.responseHandler().onError(new SdkCancellationException("The request was cancelled"));
            }
        });

        return future;
    }

    private void handleFailure(Throwable cause,
                               CompletableFuture<Void> executeFuture,
                               SdkAsyncHttpResponseHandler responseHandler) {
        try {
            responseHandler.onError(cause);
        } catch (Exception e) {
            log.error(() -> String.format("SdkAsyncHttpResponseHandler %s throw an exception in onError",
                                          responseHandler.toString()), e);
        }

        executeFuture.completeExceptionally(cause);
    }

    private static HttpRequestBase toCrtRequest(AsyncExecuteRequest asyncRequest, AwsCrtAsyncHttpStreamAdapter crtToSdkAdapter,
                                                Protocol protocol) {

        URI uri = asyncRequest.request().getUri();
        SdkHttpRequest sdkRequest = asyncRequest.request();

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.length() == 0) {
            encodedPath = "/";
        }

        String encodedQueryString = sdkRequest.encodedQueryParameters()
                                              .map(value -> "?" + value)
                                              .orElse("");
        String path = encodedPath + encodedQueryString;

        if (protocol == Protocol.HTTP1_1) {
            HttpHeader[] crtHeaderArray = asArray(createHttp1HeaderList(uri, asyncRequest));
            return new HttpRequest(method, path, crtHeaderArray, crtToSdkAdapter);
        }

        HttpHeader[] crtHeaderArray = asArray(createHttp2HeaderList(uri, asyncRequest, path));
        return new Http2Request(crtHeaderArray, crtToSdkAdapter);
    }

    private static HttpHeader[] asArray(List<HttpHeader> crtHeaderList) {
        return crtHeaderList.toArray(new HttpHeader[0]);
    }

    private static List<HttpHeader> createHttp2HeaderList(URI uri, AsyncExecuteRequest asyncRequest,
                                                          String path) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        // worst case we may add 3 more headers here
        List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.numHeaders() + 3);
        crtHeaderList.add(new HttpHeader(":method", sdkRequest.method().name()));
        crtHeaderList.add(new HttpHeader(":path", path));
        crtHeaderList.add(new HttpHeader(":scheme", uri.getScheme()));
        crtHeaderList.add(new HttpHeader(":authority", uri.getHost()));

        Optional<Long> contentLength = asyncRequest.requestContentPublisher().contentLength();
        if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent() && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
        }

        sdkRequest.forEachHeader((key, value) -> {
            value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaderList::add);
        });

        return crtHeaderList;
    }

    private static List<HttpHeader> createHttp1HeaderList(URI uri, AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        // worst case we may add 3 more headers here
        List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.numHeaders() + 3);

        // Set Host Header if needed
        if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
        }

        // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
        if (!sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE));
        }

        // Set Content-Length if needed
        Optional<Long> contentLength = asyncRequest.requestContentPublisher().contentLength();
        if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent() && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
        }

        // Add the rest of the Headers
        sdkRequest.forEachHeader((key, value) -> {
            value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaderList::add);
        });

        return crtHeaderList;
    }
}
