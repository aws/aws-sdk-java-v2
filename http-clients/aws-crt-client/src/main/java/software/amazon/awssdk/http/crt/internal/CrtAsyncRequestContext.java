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
import software.amazon.awssdk.crt.http.HttpStreamManager;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class CrtAsyncRequestContext {
    private final AsyncExecuteRequest request;
    private final long readBufferSize;
    private final HttpStreamManager crtConnPool;
    private final MetricCollector metricCollector;
    private final Protocol protocol;

    private CrtAsyncRequestContext(Builder builder) {
        this.request = builder.request;
        this.readBufferSize = builder.readBufferSize;
        this.crtConnPool = builder.crtConnPool;
        this.metricCollector = request.metricCollector().orElse(null);
        this.protocol = builder.protocol;
    }

    public static Builder builder() {
        return new Builder();
    }

    public AsyncExecuteRequest sdkRequest() {
        return request;
    }

    public long readBufferSize() {
        return readBufferSize;
    }

    public Protocol protocol() {
        return protocol;
    }

    public HttpStreamManager crtConnPool() {
        return crtConnPool;
    }

    public MetricCollector metricCollector() {
        return metricCollector;
    }

    public static final class Builder {
        private AsyncExecuteRequest request;
        private long readBufferSize;
        private HttpStreamManager crtConnPool;
        private Protocol protocol;

        private Builder() {
        }

        public Builder request(AsyncExecuteRequest request) {
            this.request = request;
            return this;
        }

        public Builder readBufferSize(long readBufferSize) {
            this.readBufferSize = readBufferSize;
            return this;
        }

        public Builder crtConnPool(HttpStreamManager crtConnPool) {
            this.crtConnPool = crtConnPool;
            return this;
        }

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public CrtAsyncRequestContext build() {
            return new CrtAsyncRequestContext(this);
        }

    }
}
