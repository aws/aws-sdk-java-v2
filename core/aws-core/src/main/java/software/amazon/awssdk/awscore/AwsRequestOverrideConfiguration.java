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

package software.amazon.awssdk.awscore;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Request-specific configuration overrides for AWS service clients.
 */
@SdkPublicApi
public final class AwsRequestOverrideConfiguration extends RequestOverrideConfiguration {
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private final IdentityProvider<? extends TokenIdentity> tokenIdentityProvider;

    private AwsRequestOverrideConfiguration(BuilderImpl builder) {
        super(builder);
        this.credentialsProvider = builder.awsCredentialsProvider;
        this.tokenIdentityProvider = builder.tokenIdentityProvider;
    }

    /**
     * Create a {@link AwsRequestOverrideConfiguration} from the provided {@link RequestOverrideConfiguration}.
     *
     * Given null, this will return null. Given a {@code AwsRequestOverrideConfiguration} this will return the input. Given
     * any other {@code RequestOverrideConfiguration} this will return a new {@code AwsRequestOverrideConfiguration} with all
     * the common attributes from the input copied into the result.
     */
    public static AwsRequestOverrideConfiguration from(RequestOverrideConfiguration configuration) {
        if (configuration == null) {
            return null;
        }

        if (configuration instanceof AwsRequestOverrideConfiguration) {
            return (AwsRequestOverrideConfiguration) configuration;
        }

        return new AwsRequestOverrideConfiguration.BuilderImpl(configuration).build();
    }

    /**
     * The optional {@link AwsCredentialsProvider} that will provide credentials to be used to authenticate this request.
     *
     * @return The optional {@link AwsCredentialsProvider}.
     */
    public Optional<AwsCredentialsProvider> credentialsProvider() {
        return Optional.ofNullable(CredentialUtils.toCredentialsProvider(credentialsProvider));
    }

    /**
     * The optional {@link IdentityProvider<? extends AwsCredentialsIdentity>} that will provide credentials to be used to
     * authenticate this request.
     *
     * @return The optional {@link IdentityProvider<? extends AwsCredentialsIdentity>}.
     */
    public Optional<IdentityProvider<? extends AwsCredentialsIdentity>> credentialsIdentityProvider() {
        return Optional.ofNullable(credentialsProvider);
    }

    /**
     * The optional {@link IdentityProvider<? extends TokenIdentity>} that will provide a token identity to be used to
     * authenticate this request.
     *
     * @return The optional {@link IdentityProvider<? extends  TokenIdentity >}.
     */
    public Optional<IdentityProvider<? extends TokenIdentity>> tokenIdentityProvider() {
        return Optional.ofNullable(tokenIdentityProvider);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AwsRequestOverrideConfiguration that = (AwsRequestOverrideConfiguration) o;
        return Objects.equals(credentialsProvider, that.credentialsProvider) &&
               Objects.equals(tokenIdentityProvider, that.tokenIdentityProvider);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(credentialsProvider);
        hashCode = 31 * hashCode + Objects.hashCode(tokenIdentityProvider);
        return hashCode;
    }

    public interface Builder extends RequestOverrideConfiguration.Builder<Builder>,
                                     SdkBuilder<Builder, AwsRequestOverrideConfiguration> {
        /**
         * Set the optional {@link AwsCredentialsProvider} that will provide credentials to be used to authenticate this request.
         *
         * @param credentialsProvider The {@link AwsCredentialsProvider}.
         * @return This object for chaining.
         */
        default Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            return credentialsProvider((IdentityProvider<AwsCredentialsIdentity>) credentialsProvider);
        }

        /**
         * Set the optional {@link IdentityProvider<? extends AwsCredentialsIdentity>} that will provide credentials to be used
         * to authenticate this request.
         *
         * @param credentialsProvider The {@link IdentityProvider<? extends AwsCredentialsIdentity>}.
         * @return This object for chaining.
         */
        default Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            throw new UnsupportedOperationException();
        }

        /**
         * Return the optional {@link AwsCredentialsProvider} that will provide credentials to be used to authenticate this
         * request.
         *
         * @return The optional {@link AwsCredentialsProvider}.
         */
        AwsCredentialsProvider credentialsProvider();

        /**
         * Set the optional {@link IdentityProvider<? extends TokenIdentity>} that will provide a token identity to be used
         * to authenticate this request.
         *
         * @param tokenIdentityProvider The {@link IdentityProvider<? extends TokenIdentity>}.
         * @return This object for chaining.
         */
        default Builder tokenIdentityProvider(IdentityProvider<? extends TokenIdentity> tokenIdentityProvider) {
            throw new UnsupportedOperationException();
        }

        @Override
        AwsRequestOverrideConfiguration build();
    }

    private static final class BuilderImpl extends RequestOverrideConfiguration.BuilderImpl<Builder> implements Builder {

        private IdentityProvider<? extends AwsCredentialsIdentity> awsCredentialsProvider;
        private IdentityProvider<? extends TokenIdentity> tokenIdentityProvider;

        private BuilderImpl() {
        }

        private BuilderImpl(RequestOverrideConfiguration requestOverrideConfiguration) {
            super(requestOverrideConfiguration);
        }

        private BuilderImpl(AwsRequestOverrideConfiguration awsRequestOverrideConfig) {
            super(awsRequestOverrideConfig);
            this.awsCredentialsProvider = awsRequestOverrideConfig.credentialsProvider;
            this.tokenIdentityProvider = awsRequestOverrideConfig.tokenIdentityProvider;
        }

        @Override
        public Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.awsCredentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public AwsCredentialsProvider credentialsProvider() {
            return CredentialUtils.toCredentialsProvider(awsCredentialsProvider);
        }

        @Override
        public Builder tokenIdentityProvider(IdentityProvider<? extends TokenIdentity> tokenIdentityProvider) {
            this.tokenIdentityProvider = tokenIdentityProvider;
            return this;
        }

        @Override
        public AwsRequestOverrideConfiguration build() {
            return new AwsRequestOverrideConfiguration(this);
        }
    }
}
