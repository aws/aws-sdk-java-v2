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

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSamlRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} implementation that loads credentials by
 * assuming a role from STS using {@link StsClient#assumeRoleWithWebIdentity(AssumeRoleWithWebIdentityRequest)}.
 *
 * <p>
 * This credential provider caches the credentials, and will only invoke STS periodically
 * to keep the credentials "fresh". As a result, it is recommended that you create a single credentials provider of this type
 * and reuse it throughout your application. You may notice small latency increases on requests that refresh the cached
 * credentials. To avoid this latency increase, you can enable async refreshing with
 * {@link Builder#asyncCredentialUpdateEnabled(Boolean)}. If you enable this setting, you must {@link #close()} the credentials
 * provider if you are done using it, to disable the background refreshing task. If you fail to do this, your application could
 * run out of resources.
 *
 * <p>
 * Create using {@link #builder()}:
 * {@snippet :
 * StsClient stsClient = StsClient.create();
 *
 * AssumeRoleWithWebIdentityRequest assumeRoleWithWebIdentityRequest =
 *     AssumeRoleWithWebIdentityRequest.builder()
 *                                     .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
 *                                     .roleSessionName("some-session-name")
 *                                     .webIdentityToken("token-from-idp")
 *                                     .build();
 *
 * StsAssumeRoleWithSamlCredentialsProvider credentialsProvider =
 *     StsAssumeRoleWithSamlCredentialsProvider.builder() // @link substring="builder" target="#builder()"
 *                                             .stsClient(stsClient)
 *                                             .refreshRequest(assumeRoleWithWebIdentityRequest)
 *                                             .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 *}
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleWithWebIdentityCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsAssumeRoleWithWebIdentityCredentialsProvider.Builder,
                                 StsAssumeRoleWithWebIdentityCredentialsProvider> {
    private static final String PROVIDER_NAME = "StsAssumeRoleWithWebIdentityCredentialsProvider";
    private final Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequest;

    /**
     * @see #builder()
     */
    private StsAssumeRoleWithWebIdentityCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-with-web-identity-credentials-provider");
        notNull(builder.assumeRoleWithWebIdentityRequestSupplier, "Assume role with web identity request must not be null.");

        this.assumeRoleWithWebIdentityRequest = builder.assumeRoleWithWebIdentityRequestSupplier;
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
                                  PROVIDER_NAME,
                                  accountIdFromArn(assumeRoleResponse.assumedRoleUser()));
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
     * A builder (created by {@link StsAssumeRoleWithWebIdentityCredentialsProvider#builder()}) for creating a
     * {@link StsAssumeRoleWithWebIdentityCredentialsProvider}.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsAssumeRoleWithWebIdentityCredentialsProvider> {
        private Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequestSupplier;

        private Builder() {
            super(StsAssumeRoleWithWebIdentityCredentialsProvider::new);
        }

        public Builder(StsAssumeRoleWithWebIdentityCredentialsProvider provider) {
            super(StsAssumeRoleWithWebIdentityCredentialsProvider::new, provider);
            this.assumeRoleWithWebIdentityRequestSupplier = provider.assumeRoleWithWebIdentityRequest;
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

        @Override
        public Builder stsClient(StsClient stsClient) {
            return super.stsClient(stsClient);
        }

        @Override
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            return super.asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled);
        }

        @Override
        public Builder staleTime(Duration staleTime) {
            return super.staleTime(staleTime);
        }

        @Override
        public Builder prefetchTime(Duration prefetchTime) {
            return super.prefetchTime(prefetchTime);
        }

        @Override
        public StsAssumeRoleWithWebIdentityCredentialsProvider build() {
            return super.build();
        }
    }
}
