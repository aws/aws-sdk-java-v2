/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithSamlRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends a {@link AssumeRoleWithSamlRequest}
 * to the AWS Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated
 * asynchronously in the background as they get close to expiring. If the credentials are not successfully updated asynchronously
 * in the background, calls to {@link #resolveCredentials()} will begin to block in an attempt to update the credentials
 * synchronously.
 *
 * This provider creates a thread in the background to periodically update credentials. If this provider is no longer needed,
 * the background thread can be shut down using {@link #close()}.
 *
 * This is created using {@link StsAssumeRoleWithSamlCredentialsProvider#builder()}.
 */
@ThreadSafe
public class StsAssumeRoleWithSamlCredentialsProvider extends StsCredentialsProvider {
    private final AssumeRoleWithSamlRequest assumeRoleWithSamlRequest;

    /**
     * @see #builder()
     */
    private StsAssumeRoleWithSamlCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-with-saml-credentials-provider");
        Validate.notNull(builder.assumeRoleWithSamlRequest, "Assume role with SAML request must not be null.");

        this.assumeRoleWithSamlRequest = builder.assumeRoleWithSamlRequest;
    }

    /**
     * Create a builder for an {@link StsAssumeRoleWithSamlCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Credentials getUpdatedCredentials(StsClient stsClient) {
        return stsClient.assumeRoleWithSAML(assumeRoleWithSamlRequest).credentials();
    }

    @Override
    public String toString() {
        return ToString.builder("StsAssumeRoleWithSamlCredentialsProvider")
                       .add("refreshRequest", assumeRoleWithSamlRequest)
                       .build();
    }

    /**
     * A builder (created by {@link StsAssumeRoleWithSamlCredentialsProvider#builder()}) for creating a
     * {@link StsAssumeRoleWithSamlCredentialsProvider}.
     */
    @NotThreadSafe
    public static final class Builder extends BaseBuilder<Builder, StsAssumeRoleWithSamlCredentialsProvider> {
        private AssumeRoleWithSamlRequest assumeRoleWithSamlRequest;

        private Builder() {
            super(StsAssumeRoleWithSamlCredentialsProvider::new);
        }

        /**
         * Configure the {@link AssumeRoleWithSamlRequest} that should be periodically sent to the STS service to update
         * the session token when it gets close to expiring.
         *
         * @param assumeRoleWithSamlRequest The request to send to STS whenever the assumed session expires.
         * @return This object for chained calls.
         */
        public Builder refreshRequest(AssumeRoleWithSamlRequest assumeRoleWithSamlRequest) {
            this.assumeRoleWithSamlRequest = assumeRoleWithSamlRequest;
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
    }
}
