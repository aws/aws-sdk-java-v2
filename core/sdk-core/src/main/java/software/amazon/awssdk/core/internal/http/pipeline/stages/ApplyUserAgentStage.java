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
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Apply any custom user agent supplied, otherwise instrument the user agent with info about the SDK and environment.
 */
@SdkInternalApi
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {
    private static final Logger log = Logger.loggerFor(ApplyUserAgentStage.class);

    private static final String COMMA = ", ";
    private static final String SPACE = " ";

    private static final String IO = "io";
    private static final String HTTP = "http";
    private static final String CONFIG = "cfg";
    private static final String RETRY_MODE = "retry-mode";

    private static final String AWS_EXECUTION_ENV_PREFIX = "exec-env/";

    private static final String HEADER_USER_AGENT = "User-Agent";

    private final SdkClientConfiguration clientConfig;

    public ApplyUserAgentStage(HttpClientDependencies dependencies) {
        this.clientConfig = dependencies.clientConfiguration();
    }

    public static String resolveClientUserAgent(String userAgentPrefix,
                                                String internalUserAgent,
                                                ClientType clientType,
                                                SdkHttpClient syncHttpClient,
                                                SdkAsyncHttpClient asyncHttpClient,
                                                RetryPolicy retryPolicy) {
        String awsExecutionEnvironment = SdkSystemSetting.AWS_EXECUTION_ENV.getStringValue().orElse(null);

        StringBuilder userAgent = new StringBuilder(128);

        userAgent.append(StringUtils.trimToEmpty(userAgentPrefix));

        String systemUserAgent = SdkUserAgent.create().userAgent();
        if (!systemUserAgent.equals(userAgentPrefix)) {
            userAgent.append(COMMA).append(systemUserAgent);
        }

        String trimmedInternalUserAgent = StringUtils.trimToEmpty(internalUserAgent);
        if (!trimmedInternalUserAgent.isEmpty()) {
            userAgent.append(SPACE).append(trimmedInternalUserAgent);
        }

        if (!StringUtils.isEmpty(awsExecutionEnvironment)) {
            userAgent.append(SPACE).append(AWS_EXECUTION_ENV_PREFIX).append(awsExecutionEnvironment.trim());
        }

        if (clientType == null) {
            clientType = ClientType.UNKNOWN;
        }

        userAgent.append(SPACE)
                 .append(IO)
                 .append("/")
                 .append(StringUtils.lowerCase(clientType.name()));

        userAgent.append(SPACE)
                 .append(HTTP)
                 .append("/")
                 .append(SdkHttpUtils.urlEncode(clientName(clientType, syncHttpClient, asyncHttpClient)));

        String retryMode = retryPolicy.retryMode().toString();

        userAgent.append(SPACE)
                 .append(CONFIG)
                 .append("/")
                 .append(RETRY_MODE)
                 .append("/")
                 .append(StringUtils.lowerCase(retryMode));

        return userAgent.toString();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        return request.putHeader(HEADER_USER_AGENT, getUserAgent(clientConfig, context.requestConfig().apiNames()));
    }

    private String getUserAgent(SdkClientConfiguration config, List<ApiName> requestApiNames) {
        String clientUserAgent = clientConfig.option(SdkClientOption.CLIENT_USER_AGENT);
        if (clientUserAgent == null) {
            log.warn(() -> "Client user agent configuration is missing, so request user agent will be incomplete.");
            clientUserAgent = "";
        }
        StringBuilder userAgent = new StringBuilder(clientUserAgent);

        if (!requestApiNames.isEmpty()) {
            requestApiNames.forEach(apiName -> {
                userAgent.append(SPACE).append(apiName.name()).append("/").append(apiName.version());
            });
        }

        String userDefinedSuffix = config.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX);
        if (!StringUtils.isEmpty(userDefinedSuffix)) {
            userAgent.append(COMMA).append(userDefinedSuffix.trim());
        }

        return userAgent.toString();
    }

    private static String clientName(ClientType clientType, SdkHttpClient syncHttpClient, SdkAsyncHttpClient asyncHttpClient) {
        if (clientType == ClientType.SYNC) {
            return syncHttpClient == null ? "null" : syncHttpClient.clientName();
        }

        if (clientType == ClientType.ASYNC) {
            return asyncHttpClient == null ? "null" : asyncHttpClient.clientName();
        }

        return ClientType.UNKNOWN.name();
    }
}
