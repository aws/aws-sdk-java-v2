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

package software.amazon.awssdk.core.internal.crac;

import java.io.ByteArrayInputStream;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * An {@link SdkHttpClient} that returns caller-supplied response bytes without performing any network I/O.
 *
 * <p>
 * Each {@link ExecutableHttpRequest#call()} returns the configured status code and a fresh stream over the response bytes.
 */
@SdkInternalApi
@Immutable
@ThreadSafe
public final class CannedResponseHttpClient implements SdkHttpClient {

    private static final int DEFAULT_STATUS_CODE = 200;
    private static final byte[] EMPTY_BODY = new byte[0];

    private final byte[] responseBody;
    private final int statusCode;

    private CannedResponseHttpClient(DefaultBuilder builder) {
        this.responseBody = builder.responseBody == null ? EMPTY_BODY : builder.responseBody.clone();
        this.statusCode = builder.statusCode == null ? DEFAULT_STATUS_CODE : builder.statusCode;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        return new CannedExecutableRequest();
    }

    @Override
    public String clientName() {
        return "CannedResponseSync";
    }

    @Override
    public void close() {
    }

    public interface Builder extends SdkBuilder<Builder, CannedResponseHttpClient> {

        /**
         * The response body returned by every {@link ExecutableHttpRequest#call()}. Optional; defaults to an empty body.
         */
        Builder responseBody(byte[] responseBody);

        /**
         * The HTTP status code returned by every {@link ExecutableHttpRequest#call()}. Defaults to {@code 200}.
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
        public CannedResponseHttpClient build() {
            return new CannedResponseHttpClient(this);
        }
    }

    private final class CannedExecutableRequest implements ExecutableHttpRequest {

        @Override
        public HttpExecuteResponse call() {
            AbortableInputStream body = AbortableInputStream.create(new ByteArrayInputStream(responseBody));
            return HttpExecuteResponse.builder()
                                      .response(SdkHttpResponse.builder()
                                                               .statusCode(statusCode)
                                                               .build())
                                      .responseBody(body)
                                      .build();
        }

        @Override
        public void abort() {
        }
    }
}
