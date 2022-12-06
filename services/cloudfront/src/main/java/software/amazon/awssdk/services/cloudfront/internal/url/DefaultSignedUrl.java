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

package software.amazon.awssdk.services.cloudfront.internal.url;

import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Immutable
@ThreadSafe
@SdkInternalApi
public final class DefaultSignedUrl implements SignedUrl, ToCopyableBuilder<DefaultSignedUrl.Builder, DefaultSignedUrl> {

    private final String protocol;
    private final String domain;
    private final String encodedPath;
    private final String url;

    private DefaultSignedUrl(Builder builder) {
        this.protocol = builder.protocol;
        this.domain = builder.domain;
        this.encodedPath = builder.encodedPath;
        this.url = builder.url;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultSignedUrl")
                       .add("url", url)
                       .build();
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public String domain() {
        return domain;
    }

    @Override
    public String encodedPath() {
        return encodedPath;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public SdkHttpRequest createHttpGetRequest() {
        return SdkHttpRequest.builder()
                             .encodedPath(encodedPath)
                             .host(domain)
                             .method(SdkHttpMethod.GET)
                             .protocol(protocol)
                             .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSignedUrl signedUrl = (DefaultSignedUrl) o;
        return Objects.equals(protocol, signedUrl.protocol)
               && Objects.equals(domain, signedUrl.domain)
               && Objects.equals(encodedPath, signedUrl.encodedPath)
               && Objects.equals(url, signedUrl.url);
    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (encodedPath != null ? encodedPath.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, DefaultSignedUrl> {
        private String protocol;
        private String domain;
        private String encodedPath;
        private String url;

        private Builder() {
        }

        private Builder(DefaultSignedUrl signedUrl) {
            this.protocol = signedUrl.protocol;
            this.domain = signedUrl.domain;
            this.encodedPath = signedUrl.encodedPath;
            this.url = signedUrl.url;
        }

        /**
         * Configure the protocol
         */
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Configure the domain
         */
        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * Configure the encoded path
         */
        public Builder encodedPath(String encodedPath) {
            this.encodedPath = encodedPath;
            return this;
        }

        /**
         * Configure the signed URL
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        @Override
        public DefaultSignedUrl build() {
            return new DefaultSignedUrl(this);
        }
    }

}
