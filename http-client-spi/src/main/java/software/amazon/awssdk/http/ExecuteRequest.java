/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Request object containing the parameters necessary to make a synchronous HTTP request.
 *
 * @see SdkHttpClient
 */
@SdkPublicApi
public final class ExecuteRequest {

    private final SdkHttpFullRequest request;

    private ExecuteRequest(BuilderImpl builder) {
        this.request = builder.request;
    }

    /**
     * @return The HTTP request.
     */
    public SdkHttpFullRequest httpRequest() {
        return request;
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
        Builder request(SdkHttpFullRequest request);

        ExecuteRequest build();
    }

    private static class BuilderImpl implements Builder {
        private SdkHttpFullRequest request;

        @Override
        public Builder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public ExecuteRequest build() {
            return new ExecuteRequest(this);
        }
    }
}
