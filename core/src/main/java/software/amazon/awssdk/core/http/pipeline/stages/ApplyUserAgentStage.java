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

package software.amazon.awssdk.core.http.pipeline.stages;

import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.RequestClientOptions;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.config.AdvancedClientOption;
import software.amazon.awssdk.core.config.ClientConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.http.HttpClientDependencies;
import software.amazon.awssdk.core.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.util.UserAgentUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Apply any custom user agent supplied, otherwise instrument the user agent with info about the SDK and environment.
 */
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {
    private static final String COMMA = ", ";
    private static final String SPACE = " ";

    private static final String AWS_EXECUTION_ENV_PREFIX = "exec-env/";

    private static final String HEADER_USER_AGENT = "User-Agent";

    private final ClientConfiguration clientConfig;

    public ApplyUserAgentStage(HttpClientDependencies dependencies) {
        this.clientConfig = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        RequestClientOptions opts = context.requestConfig().getRequestClientOptions();
        String userAgent = opts != null
                           ? getUserAgent(clientConfig, opts.getClientMarker(RequestClientOptions.Marker.USER_AGENT))
                           : getUserAgent(clientConfig, null);

        return request.header(HEADER_USER_AGENT, userAgent);
    }

    private String getUserAgent(ClientConfiguration config, String requestUserAgent) {
        ClientOverrideConfiguration overrideConfig = config.overrideConfiguration();
        String userDefinedPrefix = overrideConfig.advancedOption(AdvancedClientOption.USER_AGENT_PREFIX);
        String userDefinedSuffix = overrideConfig.advancedOption(AdvancedClientOption.USER_AGENT_SUFFIX);
        String awsExecutionEnvironment = AwsSystemSetting.AWS_EXECUTION_ENV.getStringValue().orElse(null);

        StringBuilder userAgent = new StringBuilder(StringUtils.trimToEmpty(userDefinedPrefix));

        String systemUserAgent = UserAgentUtils.getUserAgent();
        if (!systemUserAgent.equals(userDefinedPrefix)) {
            userAgent.append(COMMA).append(systemUserAgent);
        }

        if (!StringUtils.isEmpty(userDefinedSuffix)) {
            userAgent.append(COMMA).append(userDefinedSuffix.trim());
        }

        if (!StringUtils.isEmpty(awsExecutionEnvironment)) {
            userAgent.append(SPACE).append(AWS_EXECUTION_ENV_PREFIX).append(awsExecutionEnvironment.trim());
        }

        if (!StringUtils.isEmpty(requestUserAgent)) {
            userAgent.append(SPACE).append(requestUserAgent.trim());
        }

        return userAgent.toString();
    }
}
