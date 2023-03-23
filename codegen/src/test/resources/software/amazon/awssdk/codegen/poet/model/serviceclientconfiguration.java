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

package software.amazon.awssdk.services.jsonprotocoltests.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class JsonProtocolTestsServiceClientConfiguration extends AwsServiceClientConfiguration {
    protected JsonProtocolTestsServiceClientConfiguration(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends AwsServiceClientConfiguration.Builder {
        @Override
        JsonProtocolTestsServiceClientConfiguration build();

        Builder region(Region region);

        Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);
    }

    static final class BuilderImpl extends AwsServiceClientConfiguration.BuilderImpl implements Builder {
        public BuilderImpl() {
        }

        public BuilderImpl(JsonProtocolTestsServiceClientConfiguration serviceClientConfiguration) {
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
        public JsonProtocolTestsServiceClientConfiguration build() {
            return new JsonProtocolTestsServiceClientConfiguration(this);
        }
    }
}
