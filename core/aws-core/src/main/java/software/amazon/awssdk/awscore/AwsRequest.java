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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;

/**
 * Base class for all AWS Service requests.
 */
@SdkPublicApi
public abstract class AwsRequest extends SdkRequest {
    private final AwsRequestOverrideConfiguration requestOverrideConfig;
    private final List<SdkPlugin> registeredPlugins;

    protected AwsRequest(Builder builder) {
        this.requestOverrideConfig = builder.overrideConfiguration();
        this.registeredPlugins = Collections.unmodifiableList(new ArrayList<>(builder.registeredPlugins()));
    }

    @Override
    public final Optional<AwsRequestOverrideConfiguration> overrideConfiguration() {
        return Optional.ofNullable(requestOverrideConfig);
    }

    @Override
    public List<SdkPlugin> registeredPlugins() {
        return registeredPlugins;
    }

    @Override
    public abstract Builder toBuilder();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsRequest that = (AwsRequest) o;
        return Objects.equals(requestOverrideConfig, that.requestOverrideConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(requestOverrideConfig);
    }

    public interface Builder extends SdkRequest.Builder {
        @Override
        AwsRequestOverrideConfiguration overrideConfiguration();

        /**
         * Add an optional request override configuration.
         *
         * @param awsRequestOverrideConfig The override configuration.
         * @return This object for method chaining.
         */
        Builder overrideConfiguration(AwsRequestOverrideConfiguration awsRequestOverrideConfig);


        /**
         * Add an optional request override configuration.
         *
         * @param builderConsumer A {@link Consumer} to which an empty {@link AwsRequestOverrideConfiguration.Builder} will be
         * given.
         * @return This object for method chaining.
         */
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);

        @Override
        AwsRequest build();
    }

    protected abstract static class BuilderImpl implements Builder {
        private AwsRequestOverrideConfiguration awsRequestOverrideConfig;
        private final List<SdkPlugin> registeredPlugins = new ArrayList<>();

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsRequest request) {
            request.overrideConfiguration().ifPresent(this::overrideConfiguration);
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration awsRequestOverrideConfig) {
            this.awsRequestOverrideConfig = awsRequestOverrideConfig;
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            AwsRequestOverrideConfiguration.Builder b = AwsRequestOverrideConfiguration.builder();
            builderConsumer.accept(b);
            awsRequestOverrideConfig = b.build();
            return this;
        }

        @Override
        public List<SdkPlugin> registeredPlugins() {
            return registeredPlugins;
        }

        @Override
        public Builder addPlugin(SdkPlugin plugin) {
            this.registeredPlugins.add(plugin);
            return this;
        }

        @Override
        public final AwsRequestOverrideConfiguration overrideConfiguration() {
            return awsRequestOverrideConfig;
        }
    }
}
