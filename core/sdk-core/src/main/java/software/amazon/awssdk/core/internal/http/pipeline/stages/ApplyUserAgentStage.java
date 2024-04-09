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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.useragent.IdentityProviderNameMapping;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * A stage for adding the user agent header to the request, after retrieving the current string
 * from execution attributes and adding any additional information. 
 */
@SdkInternalApi
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {

    public static final String HEADER_USER_AGENT = "User-Agent";

    private static final Logger log = Logger.loggerFor(ApplyUserAgentStage.class);

    private static final String COMMA = ", ";
    private static final String SLASH = "/";
    private static final String SPACE = " ";
    private static final String HASH = "#";
    private static final String IO = "io";
    private static final String HTTP = "http";
    private static final String CONFIG = "cfg";
    private static final String RETRY_MODE = "retry-mode";
    private static final String AUTH_HEADER = "auth-source";
    private static final String AWS_EXECUTION_ENV_PREFIX = "exec-env/";

    private static final BinaryOperator<String> API_NAMES = (name, version) -> name + "/" + version;
    private static final BinaryOperator<String> CONFIG_METADATA = (param, name) -> CONFIG + SLASH + param + HASH + name;
    private static final UnaryOperator<String> AUTH_CONFIG = name -> CONFIG_METADATA.apply(AUTH_HEADER, name);

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
        return request.putHeader(HEADER_USER_AGENT, getUserAgent(clientConfig, context));
    }

    private String getUserAgent(SdkClientConfiguration config, RequestExecutionContext context) {
        String clientUserAgent = clientConfig.option(SdkClientOption.CLIENT_USER_AGENT);
        if (clientUserAgent == null) {
            log.warn(() -> "Client user agent configuration is missing, so request user agent will be incomplete.");
            clientUserAgent = "";
        }
        StringBuilder userAgent = new StringBuilder(clientUserAgent);

        //additional cfg information
        identityProviderName(context.executionAttributes())
            .ifPresent(providerName -> userAgent.append(SPACE).append(AUTH_CONFIG.apply(providerName)));

        //request API names
        requestApiNames(context.requestConfig().apiNames()).ifPresent(userAgent::append);

        //suffix
        String userDefinedSuffix = config.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX);
        if (!StringUtils.isEmpty(userDefinedSuffix)) {
            userAgent.append(COMMA).append(userDefinedSuffix.trim());
        }

        return userAgent.toString();
    }

    private static Optional<String> identityProviderName(ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes
            .getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme == null) {
            return Optional.empty();
        }
        return providerNameFromIdentity(selectedAuthScheme);
    }

    private static <T extends Identity> Optional<String> providerNameFromIdentity(SelectedAuthScheme<T> selectedAuthScheme) {
        CompletableFuture<? extends T> identityFuture = selectedAuthScheme.identity();
        T identity = CompletableFutureUtils.joinLikeSync(identityFuture);
        return identity.providerName().flatMap(IdentityProviderNameMapping::mapFrom);
    }

    private Optional<String> requestApiNames(List<ApiName> requestApiNames) {
        if (requestApiNames.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder concatenatedNames = new StringBuilder();
        requestApiNames.forEach(apiName -> concatenatedNames.append(SPACE)
                                                            .append(API_NAMES.apply(apiName.name(),
                                                                                    apiName.version())));
        return Optional.of(concatenatedNames.toString());
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
