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
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends a {@link GetFederationTokenRequest} to the
 * AWS Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated
 * asynchronously in the background as they get close to expiring. If the credentials are not successfully updated asynchronously
 * in the background, calls to {@link #getCredentials()} will begin to block in an attempt to update the credentials
 * synchronously.
 *
 * This provider creates a thread in the background to periodically update credentials. If this provider is no longer needed,
 * the background thread can be shut down using {@link #close()}.
 *
 * This is created using {@link StsGetFederationTokenCredentialsProvider#builder()}.
 */
@ThreadSafe
public class StsGetFederationTokenCredentialsProvider extends StsCredentialsProvider {
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
    protected Credentials getUpdatedCredentials(STSClient stsClient) {
        return stsClient.getFederationToken(getFederationTokenRequest).credentials();
    }

    @Override
    public String toString() {
        return ToString.builder("StsGetFederationTokenCredentialsProvider")
                       .add("refreshRequest", getFederationTokenRequest)
                       .build();
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
            return refreshRequest(GetFederationTokenRequest.builder().apply(getFederationTokenRequest).build());
        }
    }
}
