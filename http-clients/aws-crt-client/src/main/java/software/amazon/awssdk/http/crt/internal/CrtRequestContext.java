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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;

@SdkInternalApi
public final class CrtRequestContext {
    private final AsyncExecuteRequest request;
    private final int readBufferSize;
    private final HttpClientConnectionManager crtConnPool;

    private CrtRequestContext(Builder builder) {
        this.request = builder.request;
        this.readBufferSize = builder.readBufferSize;
        this.crtConnPool = builder.crtConnPool;
    }

    public static Builder builder() {
        return new Builder();
    }

    public AsyncExecuteRequest sdkRequest() {
        return request;
    }

    public int readBufferSize() {
        return readBufferSize;
    }

    public HttpClientConnectionManager crtConnPool() {
        return crtConnPool;
    }

    public static class Builder {
        private AsyncExecuteRequest request;
        private int readBufferSize;
        private HttpClientConnectionManager crtConnPool;

        private Builder() {
        }

        public Builder request(AsyncExecuteRequest request) {
            this.request = request;
            return this;
        }

        public Builder readBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
            return this;
        }

        public Builder crtConnPool(HttpClientConnectionManager crtConnPool) {
            this.crtConnPool = crtConnPool;
            return this;
        }

        public CrtRequestContext build() {
            return new CrtRequestContext(this);
        }
    }
}
