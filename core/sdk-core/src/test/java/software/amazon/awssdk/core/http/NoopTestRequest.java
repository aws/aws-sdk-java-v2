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

package software.amazon.awssdk.core.http;

import java.util.Optional;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;

public class NoopTestRequest extends SdkRequest {

    private final SdkRequestOverrideConfiguration requestOverrideConfig;

    private NoopTestRequest(Builder builder) {
        this.requestOverrideConfig = builder.overrideConfiguration();

    }

    @Override
    public Optional<SdkRequestOverrideConfiguration> overrideConfiguration() {
        return Optional.ofNullable(requestOverrideConfig);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkRequest.Builder {
        @Override
        NoopTestRequest build();

        @Override
        SdkRequestOverrideConfiguration overrideConfiguration();

        Builder overrideConfiguration(SdkRequestOverrideConfiguration requestOverrideConfig);
    }

    private static class BuilderImpl implements Builder {
        private SdkRequestOverrideConfiguration requestOverrideConfig;

        @Override
        public SdkRequestOverrideConfiguration overrideConfiguration() {
            return requestOverrideConfig;
        }

        public Builder overrideConfiguration(SdkRequestOverrideConfiguration requestOverrideConfig) {
            this.requestOverrideConfig = requestOverrideConfig;
            return this;
        }

        @Override
        public NoopTestRequest build() {
            return new NoopTestRequest(this);
        }
    }
}
