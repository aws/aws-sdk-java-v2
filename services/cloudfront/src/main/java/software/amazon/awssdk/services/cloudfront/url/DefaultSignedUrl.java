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

package software.amazon.awssdk.services.cloudfront.url;

import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudfront.internal.url.SignedUrl;

@Immutable
@ThreadSafe
@SdkPublicApi
public final class DefaultSignedUrl implements SignedUrl {

    private final String url;

    private DefaultSignedUrl(DefaultBuilder builder) {
        this.url = builder.url;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
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
        return Objects.equals(url, signedUrl.url);
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CloudFront Signed URL: " + url;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public SdkHttpRequest generateHttpRequest() {
        String protocol = url.substring(0, url.indexOf("://"));
        String domain = url.substring(url.indexOf("://") + 3, url.indexOf("cloudfront.net") + 14);
        String encodedPath = url.substring(url.indexOf("cloudfront.net") + 15);

        return SdkHttpRequest.builder()
                             .encodedPath(encodedPath)
                             .host(domain)
                             .method(SdkHttpMethod.GET)
                             .protocol(protocol)
                             .build();
    }

    private static final class DefaultBuilder implements SignedUrl.Builder {
        private String url;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DefaultSignedUrl signedUrl) {
            this.url = signedUrl.url;
        }

        @Override
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
