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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Apply any custom user agent supplied, otherwise instrument the user agent with info about the SDK and environment.
 */
@SdkInternalApi
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {
    private static final String COMMA = ", ";
    private static final String SPACE = " ";

    private static final String IO = "io";
    private static final String HTTP = "http";

    private static final String AWS_EXECUTION_ENV_PREFIX = "exec-env/";

    private static final String HEADER_USER_AGENT = "User-Agent";

    private final SdkClientConfiguration clientConfig;

    public ApplyUserAgentStage(HttpClientDependencies dependencies) {
        this.clientConfig = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        StringBuilder userAgentBuilder = getUserAgent(clientConfig, context.requestConfig().apiNames());
        String userAgent = addUserAgentSuffix(userAgentBuilder, clientConfig);
        return request.putHeader(HEADER_USER_AGENT, userAgent);
    }

    private StringBuilder getUserAgent(SdkClientConfiguration config, List<ApiName> requestApiNames) {
        String userDefinedPrefix = config.option(SdkAdvancedClientOption.USER_AGENT_PREFIX);
        String awsExecutionEnvironment = SdkSystemSetting.AWS_EXECUTION_ENV.getStringValue().orElse(null);

        StringBuilder userAgent = new StringBuilder(StringUtils.trimToEmpty(userDefinedPrefix));

        String systemUserAgent = UserAgentUtils.getUserAgent();
        if (!systemUserAgent.equals(userDefinedPrefix)) {
            userAgent.append(COMMA).append(systemUserAgent);
        }

        if (!StringUtils.isEmpty(awsExecutionEnvironment)) {
            userAgent.append(SPACE).append(AWS_EXECUTION_ENV_PREFIX).append(awsExecutionEnvironment.trim());
        }

        ClientType clientType = clientConfig.option(SdkClientOption.CLIENT_TYPE);

        if (clientType == null) {
            clientType = ClientType.UNKNOWN;
        }

        userAgent.append(SPACE)
                 .append(IO)
                 .append("/")
                 .append(StringUtils.lowerCase(clientType.name()));

        String clientName = clientName(clientType);

        userAgent.append(SPACE)
                 .append(HTTP)
                 .append("/")
                 .append(SdkHttpUtils.urlEncode(clientName));

        if (!requestApiNames.isEmpty()) {
            String requestUserAgent = requestApiNames.stream()
                    .map(n -> n.name() + "/" + n.version())
                    .collect(Collectors.joining(" "));

            userAgent.append(SPACE).append(requestUserAgent);
        }

        return userAgent;
    }

    /**
     * Only user agent suffix needs to be added in this method. Any other changes to user agent should be handled in
     * {@link #getUserAgent(SdkClientConfiguration, List)} method.
     */
    private String addUserAgentSuffix(StringBuilder userAgent, SdkClientConfiguration config) {
        String userDefinedSuffix = config.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX);

        if (!StringUtils.isEmpty(userDefinedSuffix)) {
            userAgent.append(COMMA).append(userDefinedSuffix.trim());
        }

        return userAgent.toString();
    }

    private String clientName(ClientType clientType) {
        if (clientType.equals(ClientType.SYNC)) {
            return clientConfig.option(SdkClientOption.SYNC_HTTP_CLIENT).clientName();
        }

        if (clientType.equals(ClientType.ASYNC)) {
            return clientConfig.option(SdkClientOption.ASYNC_HTTP_CLIENT).clientName();
        }

        return ClientType.UNKNOWN.name();
    }
}
