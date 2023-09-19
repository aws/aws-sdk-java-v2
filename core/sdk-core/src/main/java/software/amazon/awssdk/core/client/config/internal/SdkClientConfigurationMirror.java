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

import static software.amazon.awssdk.core.client.config.SdkClientOption.INTERNALIZE_EXTERNAL_CONFIG;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.profiles.ProfileFileSupplier;


@SdkInternalApi
public final class SdkClientConfigurationMirror {
    private SdkClientConfigurationMirror() {
    }

    /**
     * Copies the {@link ClientOverrideConfiguration} values to the configuration builder.
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
        builder.option(SdkAdvancedClientOption.SIGNER, signer);
        builder.option(SdkClientOption.SIGNER_OVERRIDDEN, signer != null);
        builder.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                       overrides.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX).orElse(null));
        builder.option(SdkAdvancedClientOption.USER_AGENT_PREFIX,
                       overrides.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX).orElse(null));
        builder.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION,
                       overrides.advancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION).orElse(null));

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

        overrides.advancedOption(SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE).ifPresent(value -> {
            builder.option(SdkClientOption.ENDPOINT_OVERRIDDEN, value);
        });

        return builder;
    }

    /**
     * Invokes all the plugins to the given configuration and returns the updated configuration.
     */
    public static SdkClientConfiguration invokePlugins(SdkClientConfiguration clientConfiguration, List<SdkPlugin> plugins) {
        SdkClientConfiguration.Builder configBuilder = clientConfiguration.toBuilder();
        InternalizeExternalConfiguration handler = clientConfiguration.option(INTERNALIZE_EXTERNAL_CONFIG);
        return handler.updateUsing(builder -> {
            for (SdkPlugin plugin : plugins) {
                plugin.configureClient(builder);
            }
        }, configBuilder);
    }
}
