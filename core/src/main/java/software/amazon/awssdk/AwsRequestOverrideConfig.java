/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk;

import software.amazon.awssdk.auth.AwsCredentialsProvider;

import java.util.Optional;

/**
 * The default per-request configuration override for AWS service clients.
 */
public class AwsRequestOverrideConfig extends SdkRequestOverrideConfig<AwsRequestOverrideConfig.Builder, AwsRequestOverrideConfig> {
    private final AwsCredentialsProvider awsCredentialsProvider;

    protected AwsRequestOverrideConfig(Builder builder) {
        super(builder);
        this.awsCredentialsProvider = builder.awsCredentialsProvider();
    }

    public final Optional<AwsCredentialsProvider> awsCredentialsProvider() {
        return Optional.ofNullable(awsCredentialsProvider);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkRequestOverrideConfig.Builder<Builder,AwsRequestOverrideConfig> {
        Builder awsCredentialsProvider(AwsCredentialsProvider awsCredentialsProvider);

        AwsCredentialsProvider awsCredentialsProvider();
    }

    protected static class BuilderImpl extends SdkRequestOverrideConfig.BuilderImpl<Builder, AwsRequestOverrideConfig> implements Builder {

        private AwsCredentialsProvider awsCredentialsProvider;

        protected BuilderImpl() {
            super(Builder.class);
        }

        protected BuilderImpl(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            super(Builder.class, awsRequestOverrideConfig);
            this.awsCredentialsProvider = awsRequestOverrideConfig.awsCredentialsProvider;
        }

        @Override
        public Builder awsCredentialsProvider(AwsCredentialsProvider awsCredentialsProvider) {
            this.awsCredentialsProvider = awsCredentialsProvider;
            return this;
        }

        @Override
        public AwsCredentialsProvider awsCredentialsProvider() {
            return awsCredentialsProvider;
        }

        @Override
        public AwsRequestOverrideConfig build() {
            return new AwsRequestOverrideConfig(this);
        }
    }
}
