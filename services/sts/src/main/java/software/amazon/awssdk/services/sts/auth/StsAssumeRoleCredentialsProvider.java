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

import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends an {@link AssumeRoleRequest} to the AWS
 * Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated asynchronously
 * in the background as they get close to expiring. If the credentials are not successfully updated asynchronously in the
 * background, calls to {@link #resolveCredentials()} will begin to block in an attempt to update the credentials synchronously.
 *
 * This provider creates a thread in the background to periodically update credentials. If this provider is no longer needed,
 * the background thread can be shut down using {@link #close()}.
 *
 * This is created using {@link StsAssumeRoleCredentialsProvider#builder()}.
 */
@SdkPublicApi
@ThreadSafe
public final class StsAssumeRoleCredentialsProvider extends StsCredentialsProvider {
    private Supplier<AssumeRoleRequest> assumeRoleRequestSupplier;

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
    protected Credentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleRequest assumeRoleRequest = assumeRoleRequestSupplier.get();
        Validate.notNull(assumeRoleRequest, "Assume role request must not be null.");
        return stsClient.assumeRole(assumeRoleRequest).credentials();
    }

    @Override
    public String toString() {
        return ToString.builder("StsAssumeRoleCredentialsProvider")
                       .add("refreshRequest", assumeRoleRequestSupplier)
                       .build();
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
    }
}
