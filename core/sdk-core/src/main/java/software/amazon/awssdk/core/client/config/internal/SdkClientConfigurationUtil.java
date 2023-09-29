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

package software.amazon.awssdk.core.client.config.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.profiles.ProfileFileSupplier;

@SdkInternalApi
public final class SdkClientConfigurationUtil {
    private SdkClientConfigurationUtil() {
    }

    /**
     * Copies the {@link ClientOverrideConfiguration} values to the configuration builder.
     * <p>
     * <b>NOTE</b> make sure to mirror the properties in the method below or create a new abstraction to avoid this coupling.
     */
    public static SdkClientConfiguration.Builder copyOverridesToConfiguration(
        ClientOverrideConfiguration overrides,
        SdkClientConfiguration.Builder builder
    ) {
        // misc
        builder.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, overrides.headers());
        builder.option(SdkClientOption.RETRY_POLICY, overrides.retryPolicy().orElse(null));
        builder.option(SdkClientOption.API_CALL_TIMEOUT, overrides.apiCallTimeout().orElse(null));
        builder.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, overrides.apiCallAttemptTimeout().orElse(null));
        builder.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, overrides.scheduledExecutorService().orElse(null));
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, overrides.executionInterceptors());
        builder.option(SdkClientOption.EXECUTION_ATTRIBUTES, overrides.executionAttributes());

        // advanced option
        Signer signer = overrides.advancedOption(SdkAdvancedClientOption.SIGNER).orElse(null);
        if (signer != null) {
            builder.option(SdkAdvancedClientOption.SIGNER, signer);
            builder.option(SdkClientOption.SIGNER_OVERRIDDEN, true);
        }
        builder.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                       overrides.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX).orElse(null));
        builder.option(SdkAdvancedClientOption.USER_AGENT_PREFIX,
                       overrides.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX).orElse(null));
        builder.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION,
                       overrides.advancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION).orElse(null));
        overrides.advancedOption(SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE).ifPresent(value -> {
            builder.option(SdkClientOption.ENDPOINT_OVERRIDDEN, value);
        });

        // profile
        builder.option(SdkClientOption.PROFILE_FILE_SUPPLIER, overrides.defaultProfileFile()
                                                                       .map(ProfileFileSupplier::fixedProfileFile)
                                                                       .orElse(null));
        builder.option(SdkClientOption.PROFILE_NAME, overrides.defaultProfileName().orElse(null));

        // misc
        builder.option(SdkClientOption.METRIC_PUBLISHERS, overrides.metricPublishers());
        builder.option(SdkAdvancedClientOption.TOKEN_SIGNER,
                       overrides.advancedOption(SdkAdvancedClientOption.TOKEN_SIGNER).orElse(null));
        builder.option(SdkClientOption.COMPRESSION_CONFIGURATION, overrides.compressionConfiguration().orElse(null));

        return builder;
    }

    /**
     * Copies the {@link SdkClientConfiguration} values to the {@link ClientOverrideConfiguration.Builder} builder
     * <p>
     * <b>NOTE</b> make sure to mirror the properties in the method above or create a new abstraction to avoid this coupling.
     */
    public static ClientOverrideConfiguration.Builder copyConfigurationToOverrides(
        ClientOverrideConfiguration.Builder clientOverrides,
        SdkClientConfiguration.Builder clientConfiguration
    ) {
        // misc
        Map<String, List<String>> additionalHeaders = clientConfiguration.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS);
        if (additionalHeaders != null) {
            clientOverrides.headers(additionalHeaders);
        }
        clientOverrides.retryPolicy(clientConfiguration.option(SdkClientOption.RETRY_POLICY));
        clientOverrides.apiCallTimeout(clientConfiguration.option(SdkClientOption.API_CALL_TIMEOUT));
        clientOverrides.apiCallAttemptTimeout(clientConfiguration.option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT));
        clientOverrides.scheduledExecutorService(clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE));
        List<ExecutionInterceptor> executionInterceptors = clientConfiguration.option(SdkClientOption.EXECUTION_INTERCEPTORS);
        if (executionInterceptors != null) {
            clientOverrides.executionInterceptors(executionInterceptors);
        }

        // advanced option
        if (Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.SIGNER_OVERRIDDEN))) {
            Signer signer = clientConfiguration.option(SdkAdvancedClientOption.SIGNER);
            clientOverrides.putAdvancedOption(SdkAdvancedClientOption.SIGNER, signer);
        }

        clientOverrides.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                                          clientConfiguration.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX));
        clientOverrides.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX,
                                          clientConfiguration.option(SdkAdvancedClientOption.USER_AGENT_PREFIX));
        clientOverrides.putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION,
                                          clientConfiguration.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION));
        clientOverrides.putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION,
                                          clientConfiguration.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION));
        clientOverrides.putAdvancedOption(SdkAdvancedClientOption.TOKEN_SIGNER,
                                          clientConfiguration.option(SdkAdvancedClientOption.TOKEN_SIGNER));

        // profile
        Optional.ofNullable(clientConfiguration.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
                .map(Supplier::get)
                .ifPresent(clientOverrides::defaultProfileFile);

        // misc
        clientOverrides.defaultProfileName(clientConfiguration.option(SdkClientOption.PROFILE_NAME));
        List<MetricPublisher> metricPublishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        if (metricPublishers != null) {
            clientOverrides.metricPublishers(metricPublishers);
        }
        clientOverrides.compressionConfiguration(clientConfiguration.option(SdkClientOption.COMPRESSION_CONFIGURATION));

        return clientOverrides;
    }

    /**
     * Executes the given list of plugins and returns the updated configuration.
     */
    public static SdkClientConfiguration invokePlugins(SdkClientConfiguration clientConfiguration,
                                                       List<SdkPlugin> plugins,
                                                       ConfigurationUpdater<SdkServiceClientConfiguration.Builder> updater
    ) {
        if (plugins.isEmpty()) {
            return clientConfiguration;
        }
        return updater.update(builder -> {
            for (SdkPlugin plugin : plugins) {
                plugin.configureClient(builder);
            }
        }, clientConfiguration.toBuilder());
    }
}
