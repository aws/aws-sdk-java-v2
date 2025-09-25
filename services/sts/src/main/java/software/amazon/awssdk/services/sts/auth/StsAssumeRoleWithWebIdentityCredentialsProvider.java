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

package software.amazon.awssdk.services.sts.auth;

import static software.amazon.awssdk.services.sts.internal.StsAuthUtils.accountIdFromArn;
import static software.amazon.awssdk.services.sts.internal.StsAuthUtils.fromStsCredentials;
import static software.amazon.awssdk.utils.Validate.notNull;

import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends an {@link AssumeRoleWithWebIdentityRequest} to the
 * AWS Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated using a
 * single calling thread (by default) or asynchronously (if {@link Builder#asyncCredentialUpdateEnabled(Boolean)} is set).
 *
 * If the credentials are not successfully updated before expiration, calls to {@link #resolveCredentials()} will block until
 * they are updated successfully.
 *
 * Users of this provider must {@link #close()} it when they are finished using it.
 *
 * This is created using {@link #builder()}.
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleWithWebIdentityCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsAssumeRoleWithWebIdentityCredentialsProvider.Builder,
                                 StsAssumeRoleWithWebIdentityCredentialsProvider> {
    private static final String PROVIDER_NAME = BusinessMetricFeatureId.CREDENTIALS_STS_ASSUME_ROLE_WEB_ID.value();
    private final Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequest;
    private final String sourceFeatureId;
    private final String providerName;

    /**
     * @see #builder()
     */
    private StsAssumeRoleWithWebIdentityCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-with-web-identity-credentials-provider");
        notNull(builder.assumeRoleWithWebIdentityRequestSupplier, "Assume role with web identity request must not be null.");

        this.assumeRoleWithWebIdentityRequest = builder.assumeRoleWithWebIdentityRequestSupplier;
        this.sourceFeatureId = builder.sourceFeatureId;
        this.providerName = StringUtils.isEmpty(builder.sourceFeatureId)
            ? PROVIDER_NAME 
            : builder.sourceFeatureId + "," + PROVIDER_NAME;
    }

    /**
     * Create a builder for an {@link StsAssumeRoleWithWebIdentityCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected AwsSessionCredentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleWithWebIdentityRequest request = assumeRoleWithWebIdentityRequest.get();
        notNull(request, "AssumeRoleWithWebIdentityRequest can't be null");
        AssumeRoleWithWebIdentityResponse assumeRoleResponse = stsClient.assumeRoleWithWebIdentity(request);
        return fromStsCredentials(assumeRoleResponse.credentials(),
                                  providerName(),
                                  accountIdFromArn(assumeRoleResponse.assumedRoleUser()));
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    String providerName() {
        return this.providerName;
    }

    /**
     * A builder (created by {@link StsAssumeRoleWithWebIdentityCredentialsProvider#builder()}) for creating a
     * {@link StsAssumeRoleWithWebIdentityCredentialsProvider}.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsAssumeRoleWithWebIdentityCredentialsProvider> {
        private Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequestSupplier;
        private String sourceFeatureId;

        private Builder() {
            super(StsAssumeRoleWithWebIdentityCredentialsProvider::new);
        }

        public Builder(StsAssumeRoleWithWebIdentityCredentialsProvider provider) {
            super(StsAssumeRoleWithWebIdentityCredentialsProvider::new, provider);
            this.assumeRoleWithWebIdentityRequestSupplier = provider.assumeRoleWithWebIdentityRequest;
            this.sourceFeatureId = provider.sourceFeatureId;
        }

        /**
         * Configure the {@link AssumeRoleWithWebIdentityRequest} that should be periodically sent to the STS service to update
         * the session token when it gets close to expiring.
         *
         * @param assumeRoleWithWebIdentityRequest The request to send to STS whenever the assumed session expires.
         * @return This object for chained calls.
         */
        public Builder refreshRequest(AssumeRoleWithWebIdentityRequest assumeRoleWithWebIdentityRequest) {
            return refreshRequest(() -> assumeRoleWithWebIdentityRequest);
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleWithWebIdentityRequest)}, but takes a {@link Supplier} to supply the
         * request to STS.
         *
         * @param assumeRoleWithWebIdentityRequest A supplier
         * @return This object for chained calls.
         */
        public Builder refreshRequest(Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequest) {
            this.assumeRoleWithWebIdentityRequestSupplier = assumeRoleWithWebIdentityRequest;
            return this;
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleWithWebIdentityRequest)}, but takes a lambda to configure a new
         * {@link AssumeRoleWithWebIdentityRequest.Builder}. This removes the need to called
         * {@link AssumeRoleWithWebIdentityRequest#builder()} and {@link AssumeRoleWithWebIdentityRequest.Builder#build()}.
         */
        public Builder refreshRequest(Consumer<AssumeRoleWithWebIdentityRequest.Builder> assumeRoleWithWebIdentityRequest) {
            return refreshRequest(AssumeRoleWithWebIdentityRequest.builder().applyMutation(assumeRoleWithWebIdentityRequest)
                                                                  .build());
        }

        /**
         * Configure the source of this credentials provider. This is used for business metrics tracking
         * to identify the credential provider chain.
         * 
         * <p><b>Note:</b> This method is primarily intended for use by AWS SDK internal components
         * and should not be used directly by external users.</p>
         *
         * @param sourceFeatureId The source identifier for business metrics tracking.
         * @return This object for chained calls.
         */
        public Builder sourceFeatureId(String sourceFeatureId) {
            this.sourceFeatureId = sourceFeatureId;
            return this;
        }

        @Override
        public StsAssumeRoleWithWebIdentityCredentialsProvider build() {
            return super.build();
        }
    }
}
