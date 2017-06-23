/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.runtime.auth;

import java.net.URI;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkProtectedApi
public class SignerProviderContext {

    private final URI uri;
    private final boolean isRedirect;
    private final SdkHttpFullRequest request;
    private final RequestConfig requestConfig;

    private SignerProviderContext(Builder builder) {
        this.uri = builder.uri;
        this.isRedirect = builder.isRedirect;
        this.request = builder.request;
        this.requestConfig = builder.requestConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public URI getUri() {
        return uri;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public SdkHttpFullRequest getRequest() {
        return request;
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    public static class Builder {

        private URI uri;
        private boolean isRedirect;
        private SdkHttpFullRequest request;
        private RequestConfig requestConfig;

        private Builder() {
        }

        public Builder withUri(final URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder withIsRedirect(final boolean withIsRedirect) {
            this.isRedirect = withIsRedirect;
            return this;
        }

        public Builder withRequest(final SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        public Builder withRequestConfig(final RequestConfig requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        public SignerProviderContext build() {
            return new SignerProviderContext(this);
        }

    }
}
