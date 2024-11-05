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

import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends an {@link AssumeRoleRequest} to the AWS
 * Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated using a single
 * calling thread (by default) or asynchronously (if {@link Builder#asyncCredentialUpdateEnabled(Boolean)} is set).
 *
 * If the credentials are not successfully updated before expiration, calls to {@link #resolveCredentials()} will block until
 * they are updated successfully.
 *
 * Users of this provider must {@link #close()} it when they are finished using it.
 *
 * This is created using {@link StsAssumeRoleCredentialsProvider#builder()}.
 *
 * {@snippet :
 * import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
 *
 * IdentityProvider<AwsCredentialsIdentity>
 *
 * StsClient stsClient = StsClient.builder()
 *                                .credentialsProvider()
 *                                .build();
 *
 * IdentityProvider<AwsCredentialsIdentity> roleCredentials =
 *     StsAssumeRoleCredentialsProvider.builder()
 *                                     .stsClient(StsClient.builder().)
 *}
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsAssumeRoleCredentialsProvider.Builder, StsAssumeRoleCredentialsProvider> {
    private static final String PROVIDER_NAME = "StsAssumeRoleCredentialsProvider";
    private final Supplier<AssumeRoleRequest> assumeRoleRequestSupplier;

    /**
     * @see #builder()
     */
    private StsAssumeRoleCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-credentials-provider");
        Validate.notNull(builder.assumeRoleRequestSupplier, "Assume role request must not be null.");

        this.assumeRoleRequestSupplier = builder.assumeRoleRequestSupplier;
    }

    /**
     * Create a builder for an {@link StsAssumeRoleCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected AwsSessionCredentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleRequest assumeRoleRequest = assumeRoleRequestSupplier.get();
        Validate.notNull(assumeRoleRequest, "Assume role request must not be null.");
        AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        return fromStsCredentials(assumeRoleResponse.credentials(),
                                  PROVIDER_NAME,
                                  accountIdFromArn(assumeRoleResponse.assumedRoleUser()));
    }

    @Override
    public String toString() {
        return ToString.builder(PROVIDER_NAME)
                       .add("refreshRequest", assumeRoleRequestSupplier)
                       .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    String providerName() {
        return PROVIDER_NAME;
    }

    /**
     * A builder (created by {@link StsAssumeRoleCredentialsProvider#builder()}) for creating a
     * {@link StsAssumeRoleCredentialsProvider}.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsAssumeRoleCredentialsProvider> {
        private Supplier<AssumeRoleRequest> assumeRoleRequestSupplier;

        private Builder() {
            super(StsAssumeRoleCredentialsProvider::new);
        }

        private Builder(StsAssumeRoleCredentialsProvider provider) {
            super(StsAssumeRoleCredentialsProvider::new, provider);
            this.assumeRoleRequestSupplier = provider.assumeRoleRequestSupplier;
        }

        /**
         * Configure the {@link AssumeRoleRequest} that should be periodically sent to the STS service to update the assumed
         * credentials.
         *
         * @param assumeRoleRequest The request to send to STS whenever the assumed session expires.
         * @return This object for chained calls.
         */
        public Builder refreshRequest(AssumeRoleRequest assumeRoleRequest) {
            return refreshRequest(() -> assumeRoleRequest);
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleRequest)}, but takes a {@link Supplier} to supply the request to
         * STS.
         *
         * @param assumeRoleRequestSupplier A supplier
         * @return This object for chained calls.
         */
        public Builder refreshRequest(Supplier<AssumeRoleRequest> assumeRoleRequestSupplier) {
            this.assumeRoleRequestSupplier = assumeRoleRequestSupplier;
            return this;
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleRequest)}, but takes a lambda to configure a new
         * {@link AssumeRoleRequest.Builder}. This removes the need to called {@link AssumeRoleRequest#builder()} and
         * {@link AssumeRoleRequest.Builder#build()}.
         */
        public Builder refreshRequest(Consumer<AssumeRoleRequest.Builder> assumeRoleRequest) {
            return refreshRequest(AssumeRoleRequest.builder().applyMutation(assumeRoleRequest).build());
        }

        @Override
        public StsAssumeRoleCredentialsProvider build() {
            return super.build();
        }

    }
}
