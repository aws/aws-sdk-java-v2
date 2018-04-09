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

package software.amazon.awssdk.core;

import java.util.Optional;

import software.amazon.awssdk.core.auth.AwsCredentialsProvider;

/**
 * Request-specific configuration overrides for AWS service clients.
 */
public final class AwsRequestOverrideConfig extends SdkRequestOverrideConfig {
    private final AwsCredentialsProvider credentialsProvider;

    private AwsRequestOverrideConfig(Builder builder) {
        super(builder);
        this.credentialsProvider = builder.credentialsProvider();
    }

    /**
     * The optional {@link AwsCredentialsProvider} that will provide credentials to be used to authenticate this request.

     * @return The optional {@link AwsCredentialsProvider}.
     */
    public Optional<AwsCredentialsProvider> credentialsProvider() {
        return Optional.ofNullable(credentialsProvider);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkRequestOverrideConfig.Builder<Builder> {
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

         * @return The optional {@link AwsCredentialsProvider}.
         */
        AwsCredentialsProvider credentialsProvider();

        @Override
        AwsRequestOverrideConfig build();
    }

    private static final class BuilderImpl extends SdkRequestOverrideConfig.BuilderImpl<Builder> implements Builder {

        private AwsCredentialsProvider awsCredentialsProvider;


        private BuilderImpl() {
        }

        private BuilderImpl(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            super(awsRequestOverrideConfig);
            this.awsCredentialsProvider = awsRequestOverrideConfig.credentialsProvider;
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
        public AwsRequestOverrideConfig build() {
            return new AwsRequestOverrideConfig(this);
        }
    }
}
