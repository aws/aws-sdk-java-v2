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

import software.amazon.awssdk.annotations.Immutable;

/**
 * Base per-request override configuration for all SDK requests.
 */
@Immutable
public final class SdkRequestOverrideConfig extends RequestOverrideConfig {

    private SdkRequestOverrideConfig(Builder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends RequestOverrideConfig.Builder<Builder> {
        @Override
        SdkRequestOverrideConfig build();
    }

    private static final class BuilderImpl extends RequestOverrideConfig.BuilderImpl<Builder> implements Builder {

        private BuilderImpl() {
        }

        private BuilderImpl(SdkRequestOverrideConfig sdkRequestOverrideConfig) {
            super(sdkRequestOverrideConfig);
        }

        @Override
        public SdkRequestOverrideConfig build() {
            return new SdkRequestOverrideConfig(this);
        }
    }
}
