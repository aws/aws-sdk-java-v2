/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;

/**
 * Class to expose the service client settings to the user. Implementation of {@link AwsServiceClientConfiguration}
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class CodeCatalystServiceClientConfiguration extends AwsServiceClientConfiguration {
    private CodeCatalystServiceClientConfiguration(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * A builder for creating a {@link CodeCatalystServiceClientConfiguration}
     */
    public interface Builder extends AwsServiceClientConfiguration.Builder {
        @Override
        CodeCatalystServiceClientConfiguration build();

        /**
         * Configure the region
         */
        @Override
        Builder region(Region region);

        /**
         * Configure the endpointOverride
         */
        @Override
        Builder endpointOverride(URI endpointOverride);

        /**
         * Configure the client override configuration
         */
        @Override
        Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);
    }

    private static final class BuilderImpl extends AwsServiceClientConfiguration.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(CodeCatalystServiceClientConfiguration serviceClientConfiguration) {
            super(serviceClientConfiguration);
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
            this.overrideConfiguration = clientOverrideConfiguration;
            return this;
        }

        @Override
        public Builder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        @Override
        public CodeCatalystServiceClientConfiguration build() {
            return new CodeCatalystServiceClientConfiguration(this);
        }
    }
}
