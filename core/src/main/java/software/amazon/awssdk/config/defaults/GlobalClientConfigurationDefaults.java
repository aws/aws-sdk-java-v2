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

package software.amazon.awssdk.config.defaults;

import static software.amazon.awssdk.config.AdvancedClientOption.USER_AGENT_PREFIX;
import static software.amazon.awssdk.config.AdvancedClientOption.USER_AGENT_SUFFIX;
import static software.amazon.awssdk.config.InternalAdvancedClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED;
import static software.amazon.awssdk.utils.CollectionUtils.mergeLists;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.util.UserAgentUtils;

/**
 * A decorator for {@link ClientConfiguration} that adds global default values. This is the lowest-priority configuration
 * decorator that attempts to fill in any required values that higher-priority configurations (eg. service-specific
 * configurations or customer-provided configurations) haven't already overridden.
 */
@SdkInternalApi
public final class GlobalClientConfigurationDefaults extends ClientConfigurationDefaults {

    @ReviewBeforeRelease("Load test this to make sure it's appropriate.")
    public static final int DEFAULT_ASYNC_POOL_SIZE = 1;

    @Override
    protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
        ClientOverrideConfiguration configuration = builder.build();
        builder.gzipEnabled(applyDefault(configuration.gzipEnabled(), () -> false));

        builder.advancedOption(USER_AGENT_PREFIX,
                               applyDefault(configuration.advancedOption(USER_AGENT_PREFIX), UserAgentUtils::getUserAgent));
        builder.advancedOption(USER_AGENT_SUFFIX, applyDefault(configuration.advancedOption(USER_AGENT_SUFFIX), () -> ""));
        builder.advancedOption(CRC32_FROM_COMPRESSED_DATA_ENABLED,
                               applyDefault(configuration.advancedOption(CRC32_FROM_COMPRESSED_DATA_ENABLED), () -> false));

        builder.retryPolicy(applyDefault(configuration.retryPolicy(), () ->
                new RetryPolicyAdapter(PredefinedRetryPolicies.DEFAULT)));

        // Put global interceptors before the ones currently configured.
        List<ExecutionInterceptor> globalInterceptors = new ClasspathInterceptorChainFactory().getGlobalInterceptors();
        builder.lastExecutionInterceptors(mergeLists(globalInterceptors, configuration.lastExecutionInterceptors()));
    }

    @Override
    protected ScheduledExecutorService getAsyncExecutorDefault() {
        return Executors.newScheduledThreadPool(DEFAULT_ASYNC_POOL_SIZE);
    }
}
