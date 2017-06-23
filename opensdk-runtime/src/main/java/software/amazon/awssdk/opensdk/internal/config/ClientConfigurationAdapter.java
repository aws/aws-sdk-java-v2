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

package software.amazon.awssdk.opensdk.internal.config;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Adapter that adapts {@link ApiGatewayClientConfiguration} to {@link LegacyClientConfiguration}.
 */
@SdkInternalApi
public class ClientConfigurationAdapter {

    /**
     * Helper method that takes in a custom {@link ApiGatewayClientConfiguration} object and
     * a service default {@link LegacyClientConfiguration} object,
     * adapts it to a target {@link LegacyClientConfiguration} object and returns it.
     * <p>
     * If value is not present for a property in customConfiguration object,
     * then the value from the defaultConfiguration is used.
     * </p>
     *
     * @param customConfiguration The adaptee which is a {@link ApiGatewayClientConfiguration} object.
     * @param defaultConfiguration The configuration to use for options that are not set in customConfiguration.
     * @return The target {@link LegacyClientConfiguration} object.
     */
    public static LegacyClientConfiguration adapt(ApiGatewayClientConfiguration customConfiguration,
                                                  LegacyClientConfiguration defaultConfiguration) {
        LegacyClientConfiguration adaptedConfiguration = new LegacyClientConfiguration(defaultConfiguration);

        customConfiguration.getProxyConfiguration().ifPresent(
            proxyConfiguration -> {
                bind(proxyConfiguration::getProtocol, adaptedConfiguration::setProtocol);
            }
        );

        customConfiguration.getTimeoutConfiguration().ifPresent(
            timeoutConfiguration -> {
                bind(timeoutConfiguration::getTotalExecutionTimeout, adaptedConfiguration::setClientExecutionTimeout);
            }
        );

        return adaptedConfiguration;
    }

    public static <T> void bind(Supplier<Optional<T>> supplier, Consumer<T> consumer) {
        supplier.get().ifPresent(consumer);
    }
}
