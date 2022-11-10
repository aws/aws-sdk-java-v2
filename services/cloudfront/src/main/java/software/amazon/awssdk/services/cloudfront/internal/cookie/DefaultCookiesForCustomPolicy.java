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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;

@Immutable
@ThreadSafe
@SdkInternalApi
public final class DefaultCookiesForCustomPolicy implements CookiesForCustomPolicy {

    private final String keyPairIdValue;
    private final String signatureValue;
    private final String resourceUrl;
    private final String policyValue;

    private DefaultCookiesForCustomPolicy(DefaultBuilder builder) {
        this.keyPairIdValue = builder.keyPairIdValue;
        this.signatureValue = builder.signatureValue;
        this.resourceUrl = builder.resourceUrl;
        this.policyValue = builder.policyValue;
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

        DefaultCookiesForCustomPolicy cookie = (DefaultCookiesForCustomPolicy) o;
        return Objects.equals(keyPairIdValue, cookie.keyPairIdValue)
               && Objects.equals(signatureValue, cookie.signatureValue)
               && Objects.equals(resourceUrl, cookie.resourceUrl)
               && Objects.equals(policyValue, cookie.policyValue);
    }

    @Override
    public int hashCode() {
        int result = keyPairIdValue != null ? keyPairIdValue.hashCode() : 0;
        result = 31 * result + (signatureValue != null ? signatureValue.hashCode() : 0);
        result = 31 * result + (resourceUrl != null ? resourceUrl.hashCode() : 0);
        result = 31 * result + (policyValue != null ? policyValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CloudFront Cookies for Custom Policy:\n"
               + "Key-Pair-ID = " + keyPairIdValue + "\n"
               + "Signature = " + signatureValue + "\n"
               + "Policy = " + policyValue + "\n"
               + "Resource URL = " + resourceUrl;
    }

    @Override
    public String keyPairIdKey() {
        return KEY_PAIR_ID_KEY;
    }

    @Override
    public String signatureKey() {
        return SIGNATURE_KEY;
    }

    @Override
    public String policyKey() {
        return POLICY_KEY;
    }

    @Override
    public String keyPairIdValue() {
        return keyPairIdValue;
    }

    @Override
    public String signatureValue() {
        return signatureValue;
    }

    @Override
    public String policyValue() {
        return policyValue;
    }

    @Override
    public String resourceUrl() {
        return resourceUrl;
    }

    @Override
    public SdkHttpRequest generateHttpRequest() {
        return SdkHttpRequest.builder()
                             .uri(URI.create(resourceUrl))
                             .appendHeader("Cookie",
                                           cookieHeaderValue(CookieType.POLICY))
                             .appendHeader("Cookie",
                                           cookieHeaderValue(CookieType.SIGNATURE))
                             .appendHeader("Cookie",
                                           cookieHeaderValue(CookieType.KEY_PAIR_ID))
                             .method(SdkHttpMethod.GET)
                             .build();
    }

    @Override
    public String cookieHeaderValue(CookieType cookieType) {
        switch (cookieType) {
            case KEY_PAIR_ID:
                return KEY_PAIR_ID_KEY + "=" + keyPairIdValue;
            case SIGNATURE:
                return SIGNATURE_KEY + "=" + signatureValue;
            case POLICY:
                return POLICY_KEY + "=" + policyValue;
            default:
                throw SdkClientException.create("Did not provide a valid cookie type");
        }
    }

    private static final class DefaultBuilder implements CookiesForCustomPolicy.Builder {
        private String keyPairIdValue;
        private String signatureValue;
        private String policyValue;
        private String resourceUrl;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DefaultCookiesForCustomPolicy cookies) {
            this.keyPairIdValue = cookies.keyPairIdValue;
            this.signatureValue = cookies.signatureValue;
            this.policyValue = cookies.policyValue;
            this.resourceUrl = cookies.resourceUrl;
        }

        @Override
        public Builder keyPairId(String keyPairId) {
            this.keyPairIdValue = keyPairId;
            return this;
        }

        @Override
        public Builder signature(String signature) {
            this.signatureValue = signature;
            return this;
        }

        @Override
        public Builder policy(String policy) {
            this.policyValue = policy;
            return this;
        }

        @Override
        public Builder resourceUrl(String resourceUrl) {
            this.resourceUrl = resourceUrl;
            return this;
        }

        @Override
        public DefaultCookiesForCustomPolicy build() {
            return new DefaultCookiesForCustomPolicy(this);
        }
    }

}
