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

import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public class HttpExecuteResponse {

    private final SdkHttpResponse response;
    private final Optional<AbortableInputStream> responseBody;

    private HttpExecuteResponse(BuilderImpl builder) {
        this.response = builder.response;
        this.responseBody = builder.responseBody;
    }

    /**
     * @return The HTTP response.
     */
    public SdkHttpResponse httpResponse() {
        return response;
    }

    /**
     * @return The {@link ContentStreamProvider}.
     */
    public Optional<AbortableInputStream> responseBody() {
        return responseBody;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the HTTP response to be executed by the client.
         *
         * @param response The response.
         * @return This builder for method chaining.
         */
        Builder response(SdkHttpResponse response);

        /**
         * Set the {@link InputStream} to be returned by the client.
         * @param inputStream The {@link InputStream}
         * @return This builder for method chaining
         */
        Builder responseBody(AbortableInputStream inputStream);

        HttpExecuteResponse build();
    }

    private static class BuilderImpl implements Builder {

        private SdkHttpResponse response;
        private Optional<AbortableInputStream> responseBody = Optional.empty();

        @Override
        public Builder response(SdkHttpResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public Builder responseBody(AbortableInputStream responseBody) {
            this.responseBody = Optional.ofNullable(responseBody);
            return this;
        }

        @Override
        public HttpExecuteResponse build() {
            return new HttpExecuteResponse(this);
        }
    }
}
