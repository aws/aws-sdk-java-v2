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

import static software.amazon.awssdk.services.sts.internal.StsAuthUtils.fromStsCredentials;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.endpoints.internal.RuleArn;
import software.amazon.awssdk.services.sts.model.FederatedUser;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.sts.model.GetFederationTokenResponse;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends a {@link GetFederationTokenRequest} to the AWS
 * Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated using a single
 * calling thread (by default) or asynchronously (if {@link Builder#asyncCredentialUpdateEnabled(Boolean)} is set).
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
public class StsGetFederationTokenCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsGetFederationTokenCredentialsProvider.Builder, StsGetFederationTokenCredentialsProvider> {
    private static final String PROVIDER_NAME = "StsGetFederationTokenCredentialsProvider";

    private final GetFederationTokenRequest getFederationTokenRequest;

    /**
     * @see #builder()
     */
    private StsGetFederationTokenCredentialsProvider(Builder builder) {
        super(builder, "sts-get-federation-token-credentials-provider");
        Validate.notNull(builder.getFederationTokenRequest, "Get session token request must not be null.");

        this.getFederationTokenRequest = builder.getFederationTokenRequest;
    }

    /**
     * Create a builder for an {@link StsGetFederationTokenCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected AwsSessionCredentials getUpdatedCredentials(StsClient stsClient) {
        GetFederationTokenResponse federationToken = stsClient.getFederationToken(getFederationTokenRequest);
        return fromStsCredentials(federationToken.credentials(),
                                  PROVIDER_NAME,
                                  accountIdFromArn(federationToken.federatedUser()));
    }

    private String accountIdFromArn(FederatedUser federatedUser) {
        if (federatedUser == null) {
            return null;
        }
        RuleArn arn = RuleArn.parse(federatedUser.arn());
        if (arn == null) {
            return null;
        }
        return arn.accountId();
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
     * A builder (created by {@link StsGetFederationTokenCredentialsProvider#builder()}) for creating a
     * {@link StsGetFederationTokenCredentialsProvider}.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsGetFederationTokenCredentialsProvider> {
        private GetFederationTokenRequest getFederationTokenRequest;

        private Builder() {
            super(StsGetFederationTokenCredentialsProvider::new);
        }

        public Builder(StsGetFederationTokenCredentialsProvider provider) {
            super(StsGetFederationTokenCredentialsProvider::new, provider);
            this.getFederationTokenRequest = provider.getFederationTokenRequest;
        }

        /**
         * Configure the {@link GetFederationTokenRequest} that should be periodically sent to the STS service to update the
         * session token when it gets close to expiring.
         *
         * @param getFederationTokenRequest The request to send to STS whenever the assumed session expires.
         * @return This object for chained calls.
         */
        public Builder refreshRequest(GetFederationTokenRequest getFederationTokenRequest) {
            this.getFederationTokenRequest = getFederationTokenRequest;
            return this;
        }

        /**
         * Similar to {@link #refreshRequest(GetFederationTokenRequest)}, but takes a lambda to configure a new
         * {@link GetFederationTokenRequest.Builder}. This removes the need to called
         * {@link GetFederationTokenRequest#builder()} and {@link GetFederationTokenRequest.Builder#build()}.
         */
        public Builder refreshRequest(Consumer<GetFederationTokenRequest.Builder> getFederationTokenRequest) {
            return refreshRequest(GetFederationTokenRequest.builder().applyMutation(getFederationTokenRequest).build());
        }

        @Override
        public StsGetFederationTokenCredentialsProvider build() {
            return super.build();
        }
    }
}
