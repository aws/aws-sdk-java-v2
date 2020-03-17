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

package software.amazon.awssdk.http;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Request object containing the parameters necessary to make a synchronous HTTP request.
 *
 * @see SdkHttpClient
 */
@SdkPublicApi
public final class HttpExecuteRequest {

    private final SdkHttpRequest request;
    private final Optional<ContentStreamProvider> contentStreamProvider;

    private HttpExecuteRequest(BuilderImpl builder) {
        this.request = builder.request;
        this.contentStreamProvider = builder.contentStreamProvider;
    }

    /**
     * @return The HTTP request.
     */
    public SdkHttpRequest httpRequest() {
        return request;
    }

    /**
     * @return The {@link ContentStreamProvider}.
     */
    public Optional<ContentStreamProvider> contentStreamProvider() {
        return contentStreamProvider;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the HTTP request to be executed by the client.
         *
         * @param request The request.
         * @return This builder for method chaining.
         */
        Builder request(SdkHttpRequest request);

        /**
         * Set the {@link ContentStreamProvider} to be executed by the client.
         * @param contentStreamProvider The content stream provider
         * @return This builder for method chaining
         */
        Builder contentStreamProvider(ContentStreamProvider contentStreamProvider);

        HttpExecuteRequest build();
    }

    private static class BuilderImpl implements Builder {
        private SdkHttpRequest request;
        private Optional<ContentStreamProvider> contentStreamProvider = Optional.empty();

        @Override
        public Builder request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public Builder contentStreamProvider(ContentStreamProvider contentStreamProvider) {
            this.contentStreamProvider = Optional.ofNullable(contentStreamProvider);
            return this;
        }

        @Override
        public HttpExecuteRequest build() {
            return new HttpExecuteRequest(this);
        }
    }
}
