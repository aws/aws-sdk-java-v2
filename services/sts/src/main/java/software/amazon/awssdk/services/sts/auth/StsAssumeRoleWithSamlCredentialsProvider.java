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

import static software.amazon.awssdk.services.sts.internal.StsAuthUtils.toAwsSessionCredentials;

import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSamlRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends an {@link AssumeRoleWithSamlRequest} to the AWS
 * Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated using a single
 * calling thread (by default) or asynchronously (if {@link Builder#asyncCredentialUpdateEnabled(Boolean)} is set).
 *
 * If the credentials are not successfully updated before expiration, calls to {@link #resolveCredentials()} will block until
 * they are updated successfully.
 *
 * Users of this provider must {@link #close()} it when they are finished using it.
 *
 * This is created using {@link StsAssumeRoleWithSamlCredentialsProvider#builder()}.
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleWithSamlCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsAssumeRoleWithSamlCredentialsProvider.Builder, StsAssumeRoleWithSamlCredentialsProvider> {
    private final Supplier<AssumeRoleWithSamlRequest> assumeRoleWithSamlRequestSupplier;


    /**
     * @see #builder()
     */
    private StsAssumeRoleWithSamlCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-with-saml-credentials-provider");
        Validate.notNull(builder.assumeRoleWithSamlRequestSupplier, "Assume role with SAML request must not be null.");

        this.assumeRoleWithSamlRequestSupplier = builder.assumeRoleWithSamlRequestSupplier;
    }

    /**
     * Create a builder for an {@link StsAssumeRoleWithSamlCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected AwsSessionCredentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleWithSamlRequest assumeRoleWithSamlRequest = assumeRoleWithSamlRequestSupplier.get();
        Validate.notNull(assumeRoleWithSamlRequest, "Assume role with saml request must not be null.");
        return toAwsSessionCredentials(stsClient.assumeRoleWithSAML(assumeRoleWithSamlRequest).credentials());
    }

    @Override
    public String toString() {
        return ToString.create("StsAssumeRoleWithSamlCredentialsProvider");
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * A builder (created by {@link StsAssumeRoleWithSamlCredentialsProvider#builder()}) for creating a
     * {@link StsAssumeRoleWithSamlCredentialsProvider}.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsAssumeRoleWithSamlCredentialsProvider> {
        private Supplier<AssumeRoleWithSamlRequest> assumeRoleWithSamlRequestSupplier;

        private Builder() {
            super(StsAssumeRoleWithSamlCredentialsProvider::new);
        }

        public Builder(StsAssumeRoleWithSamlCredentialsProvider provider) {
            super(StsAssumeRoleWithSamlCredentialsProvider::new, provider);
            this.assumeRoleWithSamlRequestSupplier = provider.assumeRoleWithSamlRequestSupplier;
        }

        /**
         * Configure the {@link AssumeRoleWithSamlRequest} that should be periodically sent to the STS service to update
         * the session token when it gets close to expiring.
         *
         * @param assumeRoleWithSamlRequest The request to send to STS whenever the assumed session expires.
         * @return This object for chained calls.
         */
        public Builder refreshRequest(AssumeRoleWithSamlRequest assumeRoleWithSamlRequest) {
            return refreshRequest(() -> assumeRoleWithSamlRequest);
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleWithSamlRequest)}, but takes a {@link Supplier} to supply the request to
         * STS.
         *
         * @param assumeRoleWithSamlRequestSupplier A supplier
         * @return This object for chained calls.
         */
        public Builder refreshRequest(Supplier<AssumeRoleWithSamlRequest> assumeRoleWithSamlRequestSupplier) {
            this.assumeRoleWithSamlRequestSupplier = assumeRoleWithSamlRequestSupplier;
            return this;
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleWithSamlRequest)}, but takes a lambda to configure a new
         * {@link AssumeRoleWithSamlRequest.Builder}. This removes the need to called {@link AssumeRoleWithSamlRequest#builder()}
         * and {@link AssumeRoleWithSamlRequest.Builder#build()}.
         */
        public Builder refreshRequest(Consumer<AssumeRoleWithSamlRequest.Builder> assumeRoleWithSamlRequest) {
            return refreshRequest(AssumeRoleWithSamlRequest.builder().applyMutation(assumeRoleWithSamlRequest).build());
        }

        @Override
        public StsAssumeRoleWithSamlCredentialsProvider build() {
            return super.build();
        }
    }
}
