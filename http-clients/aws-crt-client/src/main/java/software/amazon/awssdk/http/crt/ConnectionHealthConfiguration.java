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

package software.amazon.awssdk.http.crt;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crtcore.CrtConnectionHealthConfiguration;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration that defines health checks for all connections established by
 * the {@link ConnectionHealthConfiguration}.
 *
 */
@SdkPublicApi
public final class ConnectionHealthConfiguration extends CrtConnectionHealthConfiguration
    implements ToCopyableBuilder<ConnectionHealthConfiguration.Builder, ConnectionHealthConfiguration> {

    private ConnectionHealthConfiguration(DefaultBuilder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * A builder for {@link ConnectionHealthConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CrtConnectionHealthConfiguration.Builder,
                                     CopyableBuilder<Builder, ConnectionHealthConfiguration> {

        @Override
        Builder minimumThroughputInBps(Long minimumThroughputInBps);

        @Override
        Builder minimumThroughputTimeout(Duration minimumThroughputTimeout);

        @Override
        ConnectionHealthConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultBuilder extends
                                              CrtConnectionHealthConfiguration.DefaultBuilder<DefaultBuilder> implements Builder {

        private DefaultBuilder() {
        }

        private DefaultBuilder(ConnectionHealthConfiguration configuration) {
            super(configuration);
        }

        @Override
        public ConnectionHealthConfiguration build() {
            return new ConnectionHealthConfiguration(this);
        }
    }
}
