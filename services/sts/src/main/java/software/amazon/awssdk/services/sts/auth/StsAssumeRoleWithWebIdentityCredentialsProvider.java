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

import static software.amazon.awssdk.utils.Validate.notNull;

import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.ToString;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends a {@link AssumeRoleWithWebIdentityRequest}
 * to the AWS Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated
 * asynchronously in the background as they get close to expiring. If the credentials are not successfully updated asynchronously
 * in the background, calls to {@link #resolveCredentials()} will begin to block in an attempt to update the credentials
 * synchronously.
 *
 * This provider creates a thread in the background to periodically update credentials. If this provider is no longer needed,
 * the background thread can be shut down using {@link #close()}.
 *
 * This is created using {@link StsAssumeRoleWithWebIdentityCredentialsProvider#builder()}.
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleWithWebIdentityCredentialsProvider extends StsCredentialsProvider {
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
    protected Credentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleWithWebIdentityRequest request = assumeRoleWithWebIdentityRequest.get();
        notNull(request, "AssumeRoleWithWebIdentityRequest can't be null");
        return stsClient.assumeRoleWithWebIdentity(request).credentials();
    }

    @Override
    public String toString() {
        return ToString.builder("StsAssumeRoleWithWebIdentityCredentialsProvider")
                       .add("refreshRequest", assumeRoleWithWebIdentityRequest)
                       .build();
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
         * @param assumeRoleWithWebIdentityRequestSupplier A supplier
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
    }
}
