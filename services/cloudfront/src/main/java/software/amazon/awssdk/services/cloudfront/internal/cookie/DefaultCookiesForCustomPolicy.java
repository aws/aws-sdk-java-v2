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

package software.amazon.awssdk.services.cloudfront.internal.cookie;

import java.net.URI;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.utils.ToString;

@Immutable
@ThreadSafe
@SdkInternalApi
public final class DefaultCookiesForCustomPolicy implements CookiesForCustomPolicy {

    private final String resourceUrl;
    private final String signatureHeaderValue;
    private final String keyPairIdHeaderValue;
    private final String policyHeaderValue;

    private DefaultCookiesForCustomPolicy(DefaultBuilder builder) {
        this.resourceUrl = builder.resourceUrl;
        this.signatureHeaderValue = builder.signatureHeaderValue;
        this.keyPairIdHeaderValue = builder.keyPairIdHeaderValue;
        this.policyHeaderValue = builder.policyHeaderValue;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultCookiesForCustomPolicy")
                       .add("resourceUrl", resourceUrl)
                       .add("signatureHeaderValue", signatureHeaderValue)
                       .add("keyPairIdHeaderValue", keyPairIdHeaderValue)
                       .add("policyHeaderValue", policyHeaderValue)
                       .build();
    }

    @Override
    public String resourceUrl() {
        return resourceUrl;
    }

    @Override
    public SdkHttpRequest createHttpGetRequest() {
        return SdkHttpRequest.builder()
                             .uri(URI.create(resourceUrl))
                             .appendHeader(COOKIE, policyHeaderValue())
                             .appendHeader(COOKIE, signatureHeaderValue())
                             .appendHeader(COOKIE, keyPairIdHeaderValue())
                             .method(SdkHttpMethod.GET)
                             .build();
    }

    @Override
    public String signatureHeaderValue() {
        return signatureHeaderValue;
    }

    @Override
    public String keyPairIdHeaderValue() {
        return keyPairIdHeaderValue;
    }

    @Override
    public String policyHeaderValue() {
        return policyHeaderValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultCookiesForCustomPolicy cookie = (DefaultCookiesForCustomPolicy) o;
        return Objects.equals(keyPairIdHeaderValue, cookie.keyPairIdHeaderValue)
               && Objects.equals(signatureHeaderValue, cookie.signatureHeaderValue)
               && Objects.equals(resourceUrl, cookie.resourceUrl)
               && Objects.equals(policyHeaderValue, cookie.policyHeaderValue);
    }

    @Override
    public int hashCode() {
        int result = keyPairIdHeaderValue != null ? keyPairIdHeaderValue.hashCode() : 0;
        result = 31 * result + (signatureHeaderValue != null ? signatureHeaderValue.hashCode() : 0);
        result = 31 * result + (resourceUrl != null ? resourceUrl.hashCode() : 0);
        result = 31 * result + (policyHeaderValue != null ? policyHeaderValue.hashCode() : 0);
        return result;
    }

    private static final class DefaultBuilder implements CookiesForCustomPolicy.Builder {
        private String resourceUrl;
        private String signatureHeaderValue;
        private String keyPairIdHeaderValue;
        private String policyHeaderValue;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DefaultCookiesForCustomPolicy cookies) {
            this.resourceUrl = cookies.resourceUrl;
            this.signatureHeaderValue = cookies.signatureHeaderValue;
            this.keyPairIdHeaderValue = cookies.keyPairIdHeaderValue;
            this.policyHeaderValue = cookies.policyHeaderValue;
        }

        @Override
        public Builder resourceUrl(String resourceUrl) {
            this.resourceUrl = resourceUrl;
            return this;
        }

        @Override
        public Builder signatureHeaderValue(String signatureHeaderValue) {
            this.signatureHeaderValue = signatureHeaderValue;
            return this;
        }

        @Override
        public Builder keyPairIdHeaderValue(String keyPairIdHeaderValue) {
            this.keyPairIdHeaderValue = keyPairIdHeaderValue;
            return this;
        }

        @Override
        public Builder policyHeaderValue(String policyHeaderValue) {
            this.policyHeaderValue = policyHeaderValue;
            return this;
        }

        @Override
        public DefaultCookiesForCustomPolicy build() {
            return new DefaultCookiesForCustomPolicy(this);
        }
    }

}
