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

package software.amazon.awssdk.services.cloudfront.model;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.cloudfront.internal.utils.SigningUtils;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request to generate CloudFront signed URLs or signed cookies with a custom policy
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public final class CustomSignerRequest implements CloudFrontSignerRequest,
                                                  ToCopyableBuilder<CustomSignerRequest.Builder, CustomSignerRequest> {

    private final String resourceUrl;
    private final PrivateKey privateKey;
    private final String keyPairId;
    private final Instant expirationDate;
    private final Instant activeDate;
    private final String ipRange;

    private CustomSignerRequest(DefaultBuilder builder) {
        this.resourceUrl = builder.resourceUrl;
        this.privateKey = builder.privateKey;
        this.keyPairId = builder.keyPairId;
        this.expirationDate = builder.expirationDate;
        this.activeDate = builder.activeDate;
        this.ipRange = builder.ipRange;
    }

    /**
     * Create a builder that can be used to create a {@link CustomSignerRequest}
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public String resourceUrl() {
        return resourceUrl;
    }

    @Override
    public PrivateKey privateKey() {
        return privateKey;
    }

    @Override
    public String keyPairId() {
        return keyPairId;
    }

    @Override
    public Instant expirationDate() {
        return expirationDate;
    }

    /**
     * Returns the active date, before which users will not yet be able to use the signed URL/cookie to access your private
     * content
     */
    public Instant activeDate() {
        return activeDate;
    }

    /**
     * Returns the IP range of the users allowed to access your private content
     */
    public String ipRange() {
        return ipRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomSignerRequest cookie = (CustomSignerRequest) o;
        return Objects.equals(resourceUrl, cookie.resourceUrl)
               && Objects.equals(privateKey, cookie.privateKey)
               && Objects.equals(keyPairId, cookie.keyPairId)
               && Objects.equals(expirationDate, cookie.expirationDate)
               && Objects.equals(activeDate, cookie.activeDate)
               && Objects.equals(ipRange, cookie.ipRange);
    }

    @Override
    public int hashCode() {
        int result = resourceUrl != null ? resourceUrl.hashCode() : 0;
        result = 31 * result + (privateKey != null ? privateKey.hashCode() : 0);
        result = 31 * result + (keyPairId != null ? keyPairId.hashCode() : 0);
        result = 31 * result + (expirationDate != null ? expirationDate.hashCode() : 0);
        result = 31 * result + (activeDate != null ? activeDate.hashCode() : 0);
        result = 31 * result + (ipRange != null ? ipRange.hashCode() : 0);
        return result;
    }

    @NotThreadSafe
    @SdkPublicApi
    public interface Builder extends CopyableBuilder<CustomSignerRequest.Builder, CustomSignerRequest> {

        /**
         * Configure the resource URL to be signed
         * <p>
         * The URL or path that uniquely identifies a resource within a
         * distribution. For standard distributions the resource URL will
         * be <tt>"http://" + distributionName + "/" + objectKey</tt>
         * (may also include URL parameters. For distributions with the
         * HTTPS required protocol, the resource URL must start with
         * <tt>"https://"</tt>
         */
        Builder resourceUrl(String resourceUrl);

        /**
         * Configure the private key to be used to sign the policy.
         * Takes a PrivateKey object directly
         */
        Builder privateKey(PrivateKey privateKey);

        /**
         * Configure the private key to be used to sign the policy.
         * Takes a Path to the key file, and loads it to return a PrivateKey object
         */
        Builder privateKey(Path keyFile) throws Exception;

        /**
         * Configure the ID of the key pair stored in the AWS account
         */
        Builder keyPairId(String keyPairId);

        /**
         * Configure the expiration date of the signed URL or signed cookie
         */
        Builder expirationDate(Instant expirationDate);

        /**
         * Configure the active date of the signed URL or signed cookie - for custom policies (optional field)
         */
        Builder activeDate(Instant activeDate);

        /**
         * Configure the IP range of the signed URL or signed cookie - for custom policies (optional field)
         * <p>
         * The allowed IP address range of the client making the GET
         * request, in IPv4 CIDR form (e.g. 192.168.0.1/24).
         * IPv6 format is not supported.
         */
        Builder ipRange(String ipRange);
    }

    private static final class DefaultBuilder implements Builder {
        private String resourceUrl;
        private PrivateKey privateKey;
        private String keyPairId;
        private Instant expirationDate;
        private Instant activeDate;
        private String ipRange;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CustomSignerRequest request) {
            this.resourceUrl = request.resourceUrl;
            this.privateKey = request.privateKey;
            this.keyPairId = request.keyPairId;
            this.expirationDate = request.expirationDate;
            this.activeDate = request.activeDate;
            this.ipRange = request.ipRange;
        }

        @Override
        public Builder resourceUrl(String resourceUrl) {
            this.resourceUrl = resourceUrl;
            return this;
        }

        @Override
        public Builder privateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        @Override
        public Builder privateKey(Path keyFile) throws Exception {
            this.privateKey = SigningUtils.loadPrivateKey(keyFile);
            return this;
        }

        @Override
        public Builder keyPairId(String keyPairId) {
            this.keyPairId = keyPairId;
            return this;
        }

        @Override
        public Builder expirationDate(Instant expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        @Override
        public Builder activeDate(Instant activeDate) {
            this.activeDate = activeDate;
            return this;
        }

        @Override
        public Builder ipRange(String ipRange) {
            this.ipRange = ipRange;
            return this;
        }

        @Override
        public CustomSignerRequest build() {
            return new CustomSignerRequest(this);
        }
    }

}
