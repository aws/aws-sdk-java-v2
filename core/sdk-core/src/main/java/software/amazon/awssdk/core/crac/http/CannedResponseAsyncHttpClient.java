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

package software.amazon.awssdk.core.crac.http;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.async.SimplePublisher;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * An {@link SdkAsyncHttpClient} that completes every request with caller-supplied response bytes without performing any
 * network I/O. It is the async counterpart of {@link CannedResponseHttpClient}, used to warm the async request pipeline for a
 * CRaC checkpoint.
 *
 * <p>
 * Each {@link #execute(AsyncExecuteRequest)} signals the configured status code, publishes the response bytes through a
 * {@link SimplePublisher}, and returns an already-completed future.
 */
@SdkProtectedApi
@Immutable
@ThreadSafe
public final class CannedResponseAsyncHttpClient implements SdkAsyncHttpClient {

    private static final int DEFAULT_STATUS_CODE = 200;
    private static final byte[] EMPTY_BODY = new byte[0];

    private final byte[] responseBody;
    private final int statusCode;

    private CannedResponseAsyncHttpClient(DefaultBuilder builder) {
        this.responseBody = builder.responseBody == null ? EMPTY_BODY : builder.responseBody.clone();
        this.statusCode = builder.statusCode == null ? DEFAULT_STATUS_CODE : builder.statusCode;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        request.responseHandler().onHeaders(SdkHttpResponse.builder().statusCode(statusCode).build());

        SimplePublisher<ByteBuffer> bodyPublisher = new SimplePublisher<>();
        request.responseHandler().onStream(bodyPublisher);
        if (responseBody.length > 0) {
            bodyPublisher.send(ByteBuffer.wrap(responseBody));
        }
        bodyPublisher.complete();

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String clientName() {
        return "CannedResponseAsync";
    }

    @Override
    public void close() {
    }

    public interface Builder extends SdkBuilder<Builder, CannedResponseAsyncHttpClient> {

        /**
         * The response body published by every {@link #execute(AsyncExecuteRequest)}. Optional; defaults to an empty body.
         */
        Builder responseBody(byte[] responseBody);

        /**
         * The HTTP status code signalled by every {@link #execute(AsyncExecuteRequest)}. Defaults to {@code 200}.
         */
        Builder statusCode(int statusCode);
    }

    private static final class DefaultBuilder implements Builder {

        private byte[] responseBody;
        private Integer statusCode;

        @Override
        public Builder responseBody(byte[] responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public CannedResponseAsyncHttpClient build() {
            return new CannedResponseAsyncHttpClient(this);
        }
    }
}
