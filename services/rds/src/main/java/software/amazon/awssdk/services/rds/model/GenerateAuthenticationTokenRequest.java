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

package software.amazon.awssdk.services.rds.model;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Input parameters for generating an auth token for IAM database authentication.
 */
@SdkPublicApi
public final class GenerateAuthenticationTokenRequest implements
        ToCopyableBuilder<GenerateAuthenticationTokenRequest.Builder, GenerateAuthenticationTokenRequest> {
    private final String hostname;
    private final int port;
    private final String username;
    private final Region region;
    private final AwsCredentialsProvider credentialsProvider;

    private GenerateAuthenticationTokenRequest(BuilderImpl builder) {
        this.hostname = Validate.notEmpty(builder.hostname, "hostname");
        this.port = Validate.isPositive(builder.port, "port");
        this.username = Validate.notEmpty(builder.username, "username");
        this.region = builder.region;
        this.credentialsProvider = builder.credentialsProvider;
    }

    /**
     * @return The hostname of the database to connect to
     */
    public String hostname() {
        return hostname;
    }

    /**
     * @return The port of the database to connect to
     */
    public int port() {
        return port;
    }

    /**
     * @return The username to log in as.
     */
    public String username() {
        return username;
    }

    /**
     * @return The region the database is hosted in. If specified, takes precedence over the value specified in
     * {@link RdsUtilities.Builder#region(Region)}
     */
    public Region region() {
        return region;
    }

    /**
     * @return The credentials provider to sign the IAM auth request with. If specified, takes precedence over the value
     * specified in {@link RdsUtilities.Builder#credentialsProvider(AwsCredentialsProvider)}}
     */
    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Creates a builder for {@link RdsUtilities}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * A builder for a {@link GenerateAuthenticationTokenRequest}, created with {@link #builder()}.
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, GenerateAuthenticationTokenRequest> {
        /**
         * The hostname of the database to connect to
         *
         * @return This object for method chaining
         */
        Builder hostname(String endpoint);

        /**
         * The port number the database is listening on.
         *
         * @return This object for method chaining
         */
        Builder port(int port);

        /**
         * The username to log in as.
         *
         * @return This object for method chaining
         */
        Builder username(String userName);

        /**
         * The region the database is hosted in. If specified, takes precedence over the value specified in
         * {@link RdsUtilities.Builder#region(Region)}
         *
         * @return This object for method chaining
         */
        Builder region(Region region);

        /**
         * The credentials provider to sign the IAM auth request with. If specified, takes precedence over the value
         * specified in {@link RdsUtilities.Builder#credentialsProvider(AwsCredentialsProvider)}}
         *
         * @return This object for method chaining
         */
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        @Override
        GenerateAuthenticationTokenRequest build();
    }

    private static final class BuilderImpl implements Builder {
        private String hostname;
        private int port;
        private String username;
        private Region region;
        private AwsCredentialsProvider credentialsProvider;

        private BuilderImpl() {
        }

        private BuilderImpl(GenerateAuthenticationTokenRequest request) {
            this.hostname = request.hostname;
            this.port = request.port;
            this.username = request.username;
            this.region = request.region;
            this.credentialsProvider = request.credentialsProvider;
        }

        public Builder hostname(String endpoint) {
            this.hostname = endpoint;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder username(String userName) {
            this.username = userName;
            return this;
        }

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public GenerateAuthenticationTokenRequest build() {
            return new GenerateAuthenticationTokenRequest(this);
        }
    }
}