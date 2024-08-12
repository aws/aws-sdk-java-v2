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

package software.amazon.awssdk.services.axdbfrontend.model;

import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.axdbfrontend.AxdbFrontendUtilities;
import software.amazon.awssdk.services.axdbfrontend.model.Action;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;


/**
 * Input parameters for generating an authentication token for IAM database authentication for AxdbFrontend.
 */
@SdkPublicApi
public final class GenerateAuthenticationTokenRequest implements
                                                      ToCopyableBuilder<GenerateAuthenticationTokenRequest.Builder,
                                                          GenerateAuthenticationTokenRequest> {
    // The time the IAM token is good for based on RDS. https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.IAMDBAuth.html
    private static final Duration EXPIRATION_DURATION = Duration.ofSeconds(900L);

    private final String hostname;
    private final Region region;
    private final Action action;
    private final Duration expiresIn;
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;

    private GenerateAuthenticationTokenRequest(BuilderImpl builder) {
        this.hostname = Validate.notEmpty(builder.hostname, "hostname");
        this.action = Validate.notNull(builder.action, "action");
        Validate.isTrue(this.action == Action.DB_CONNECT || this.action == Action.DB_CONNECT_SUPERUSER, "invalid action");
        this.region = builder.region;
        this.credentialsProvider = builder.credentialsProvider;
        this.expiresIn = (builder.expiresIn != null) ? builder.expiresIn :
                             EXPIRATION_DURATION;
    }

    @Override
    public String toString() {
        return ToString.builder("GenerateAuthenticationTokenRequest")
                       .add("hostname", hostname)
                       .add("region", region)
                       .add("action", action)
                       .add("expiresIn", expiresIn)
                       .add("credentialsProvider", credentialsProvider)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenerateAuthenticationTokenRequest that = (GenerateAuthenticationTokenRequest) o;
        return Objects.equals(hostname, that.hostname) &&
               Objects.equals(region, that.region) &&
               Objects.equals(action, that.action) &&
               Objects.equals(expiresIn, that.expiresIn) &&
               Objects.equals(credentialsProvider, that.credentialsProvider);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(hostname);
        hashCode = 31 * hashCode + Objects.hashCode(region);
        hashCode = 31 * hashCode + Objects.hashCode(action);
        hashCode = 31 * hashCode + Objects.hashCode(expiresIn);
        hashCode = 31 * hashCode + Objects.hashCode(credentialsProvider);
        return hashCode;
    }

    /**
     * @return The hostname of the database to connect to
     */
    public String hostname() {
        return hostname;
    }

    /**
     * @return The token expiry duration
     */
    public Duration expiresIn() {
        return expiresIn;
    }

    /**
     * @return The action to perform on the database
     * {@link Action.Action}
     */
    public Action action() {
        return action;
    }

    /**
     * @return The region the database is hosted in. If specified, takes precedence over the value specified in
     * {@link AxdbFrontendUtilities.Builder#region(Region)}
     */
    public Region region() {
        return region;
    }

    /**
     * @return The credentials provider to sign the IAM auth request with. If specified, takes precedence over the value
     * specified in {@link AxdbFrontendUtilities.Builder#credentialsProvider}}
     */
    public AwsCredentialsProvider credentialsProvider() {
        return CredentialUtils.toCredentialsProvider(credentialsProvider);
    }

    /**
     * @return The credentials provider to sign the IAM auth request with. If specified, takes precedence over the value
     * specified in {@link AxdbFrontendUtilities.Builder#credentialsProvider(AwsCredentialsProvider)}}
     */
    public IdentityProvider<? extends AwsCredentialsIdentity> credentialsIdentityProvider() {
        return credentialsProvider;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Creates a builder for {@link AxdbFrontendUtilities}.
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
         * The action to connect to the database as.
         *
         * @return This object for method chaining
         */
        Builder action(Action action);

        /**
         * The region the database is hosted in. If specified, takes precedence over the value specified in
         * {@link AxdbFrontendUtilities.Builder#region(Region)}
         *
         * @return This object for method chaining
         */
        Builder region(Region region);

        /**
         * The duration a token is valid for.
         *
         * @return This object for method chaining
         */
        Builder expiresIn(Duration expiresIn);

        /**
         * The credentials provider to sign the IAM auth request with. If specified, takes precedence over the value
         * specified in {@link AxdbFrontendUtilities.Builder#credentialsProvider)}}
         *
         * @return This object for method chaining
         */
        default Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            return credentialsProvider((IdentityProvider<? extends AwsCredentialsIdentity>) credentialsProvider);
        }

        /**
         * The credentials provider to sign the IAM auth request with. If specified, takes precedence over the value
         * specified in {@link AxdbFrontendUtilities.Builder#credentialsProvider}}
         *
         * @return This object for method chaining
         */
        default Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            throw new UnsupportedOperationException();
        }

        @Override
        GenerateAuthenticationTokenRequest build();
    }

    private static final class BuilderImpl implements Builder {
        private String hostname;
        private Action action;
        private Region region;
        private Duration expiresIn;
        private IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;

        private BuilderImpl() {
        }

        private BuilderImpl(GenerateAuthenticationTokenRequest request) {
            this.hostname = request.hostname;
            this.action = request.action;
            this.region = request.region;
            this.expiresIn = request.expiresIn;
            this.credentialsProvider = request.credentialsProvider;
        }

        @Override
        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        @Override
        public Builder expiresIn(Duration expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        @Override
        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public GenerateAuthenticationTokenRequest build() {
            return new GenerateAuthenticationTokenRequest(this);
        }
    }
}