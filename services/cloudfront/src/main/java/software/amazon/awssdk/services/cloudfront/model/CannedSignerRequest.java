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
 * Request to generate CloudFront signed URLs or signed cookies with a canned policy
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public final class CannedSignerRequest implements CloudFrontSignerRequest,
                                                  ToCopyableBuilder<CannedSignerRequest.Builder, CannedSignerRequest> {

    private final String resourceUrl;
    private final PrivateKey privateKey;
    private final String keyPairId;
    private final Instant expirationDate;

    private CannedSignerRequest(DefaultBuilder builder) {
        this.resourceUrl = builder.resourceUrl;
        this.privateKey = builder.privateKey;
        this.keyPairId = builder.keyPairId;
        this.expirationDate = builder.expirationDate;
    }

    /**
     * Create a builder that can be used to create a {@link CannedSignerRequest}
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CannedSignerRequest cookie = (CannedSignerRequest) o;
        return Objects.equals(resourceUrl, cookie.resourceUrl)
               && Objects.equals(privateKey, cookie.privateKey)
               && Objects.equals(keyPairId, cookie.keyPairId)
               && Objects.equals(expirationDate, cookie.expirationDate);
    }

    @Override
    public int hashCode() {
        int result = resourceUrl != null ? resourceUrl.hashCode() : 0;
        result = 31 * result + (privateKey != null ? privateKey.hashCode() : 0);
        result = 31 * result + (keyPairId != null ? keyPairId.hashCode() : 0);
        result = 31 * result + (expirationDate != null ? expirationDate.hashCode() : 0);
        return result;
    }

    @NotThreadSafe
    @SdkPublicApi
    public interface Builder extends CopyableBuilder<CannedSignerRequest.Builder, CannedSignerRequest> {

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
    }

    private static final class DefaultBuilder implements Builder {
        private String resourceUrl;
        private PrivateKey privateKey;
        private String keyPairId;
        private Instant expirationDate;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CannedSignerRequest request) {
            this.resourceUrl = request.resourceUrl;
            this.privateKey = request.privateKey;
            this.keyPairId = request.keyPairId;
            this.expirationDate = request.expirationDate;
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
        public CannedSignerRequest build() {
            return new CannedSignerRequest(this);
        }
    }

}
