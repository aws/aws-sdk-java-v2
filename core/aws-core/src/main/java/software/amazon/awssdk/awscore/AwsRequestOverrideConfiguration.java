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
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Request-specific configuration overrides for AWS service clients.
 */
@SdkPublicApi
public final class AwsRequestOverrideConfiguration extends RequestOverrideConfiguration {
    private final AwsCredentialsProvider credentialsProvider;
    private final SdkTokenProvider tokenProvider;

    private AwsRequestOverrideConfiguration(Builder builder) {
        super(builder);
        this.credentialsProvider = builder.credentialsProvider();
        this.tokenProvider = builder.tokenProvider();
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
        return Optional.ofNullable(credentialsProvider);
    }

    /**
     * The optional {@link SdkTokenProvider} that will provide a token to be used to authorize this request. This will
     * be used only if the requested operation uses bearer token authorization.
     *
     * @return The optional {@link SdkTokenProvider}.
     */
    public Optional<SdkTokenProvider> tokenProvider() {
        return Optional.ofNullable(tokenProvider);
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
        return Objects.equals(credentialsProvider, that.credentialsProvider)
               && Objects.equals(tokenProvider, that.tokenProvider);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(credentialsProvider);
        hashCode = 31 * hashCode + Objects.hashCode(tokenProvider);
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
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        /**
         * Return the optional {@link AwsCredentialsProvider} that will provide credentials to be used to authenticate this
         * request.
         *
         * @return The optional {@link AwsCredentialsProvider}.
         */
        AwsCredentialsProvider credentialsProvider();

        /**
         * Set the optional {@link SdkTokenProvider} that will provide a token to be used to authorize this request.
         * This will be used only if the requested operation uses bearer token authorization.
         *
         * @param tokenProvider The {@link SdkTokenProvider}.
         * @return This object for chaining.
         */
        Builder tokenProvider(SdkTokenProvider tokenProvider);

        /**
         * Return the optional {@link SdkTokenProvider} that will provide a token to be used to authorize this request.
         * This will be used only if the requested operation uses bearer token authorization.
         *
         * @return The optional {@link AwsCredentialsProvider}.
         */
        SdkTokenProvider tokenProvider();

        @Override
        AwsRequestOverrideConfiguration build();
    }

    private static final class BuilderImpl extends RequestOverrideConfiguration.BuilderImpl<Builder> implements Builder {

        private AwsCredentialsProvider awsCredentialsProvider;
        private SdkTokenProvider sdkTokenProvider;

        private BuilderImpl() {
        }

        private BuilderImpl(RequestOverrideConfiguration requestOverrideConfiguration) {
            super(requestOverrideConfiguration);
        }

        private BuilderImpl(AwsRequestOverrideConfiguration awsRequestOverrideConfig) {
            super(awsRequestOverrideConfig);
            this.awsCredentialsProvider = awsRequestOverrideConfig.credentialsProvider;
            this.sdkTokenProvider = awsRequestOverrideConfig.tokenProvider;
        }

        @Override
        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.awsCredentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public AwsCredentialsProvider credentialsProvider() {
            return awsCredentialsProvider;
        }

        @Override
        public Builder tokenProvider(SdkTokenProvider tokenProvider) {
            this.sdkTokenProvider = tokenProvider;
            return this;
        }

        @Override
        public SdkTokenProvider tokenProvider() {
            return sdkTokenProvider;
        }

        @Override
        public AwsRequestOverrideConfiguration build() {
            return new AwsRequestOverrideConfiguration(this);
        }
    }
}
