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

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.AUTH_SOURCE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.CONFIG_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SLASH;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SPACE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendSpaceAndField;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.uaPair;
import static software.amazon.awssdk.utils.StringUtils.trim;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ApiName;
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
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A stage for adding the user agent header to the request, after retrieving the current string
 * from execution attributes and adding any additional information. 
 */
@SdkInternalApi
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {

    public static final String HEADER_USER_AGENT = "User-Agent";

    private static final Logger log = Logger.loggerFor(ApplyUserAgentStage.class);

    private final SdkClientConfiguration clientConfig;

    public ApplyUserAgentStage(HttpClientDependencies dependencies) {
        this.clientConfig = dependencies.clientConfiguration();
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request,
                                              RequestExecutionContext context) throws Exception {
        String headerValue = finalizeUserAgent(context);
        return request.putHeader(HEADER_USER_AGENT, headerValue);
    }

    /**
     * The final value sent in the user agent header consists of
     * <ol>
     *     <li>an optional user provided prefix</li>
     *     <li>SDK user agent values (governed by a common specification)</li>
     *     <li>an optional set of API names, expressed as name/version pairs</li>
     *     <li>an optional user provided suffix</li>
     * </ol>
     * <p>
     * In general, usage of the optional values is discouraged since they do not follow a specification and can make
     * the user agent too long.
     * <p>
     * The SDK user agent values are constructed from static system values, client level values and request level
     * values. This method adds request level values directly after the retrieved SDK client user agent string.
     */
    private String finalizeUserAgent(RequestExecutionContext context) {
        String clientUserAgent = clientConfig.option(SdkClientOption.CLIENT_USER_AGENT);
        if (clientUserAgent == null) {
            log.warn(() -> "Client user agent configuration is missing, so request user agent will be incomplete.");
            clientUserAgent = "";
        }

        StringBuilder javaUserAgent = new StringBuilder();

        String userPrefix = trim(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_PREFIX));
        if (!StringUtils.isEmpty(userPrefix)) {
            javaUserAgent.append(userPrefix).append(SPACE);
        }

        javaUserAgent.append(clientUserAgent);

        //add remaining SDK user agent properties
        identityProviderName(context.executionAttributes()).ifPresent(
            authSource -> appendSpaceAndField(javaUserAgent, CONFIG_METADATA, uaPair(AUTH_SOURCE, authSource)));

        //treat ApiNames as an opaque set of values because it may contain user values
        Optional<String> apiNames = requestApiNames(context.requestConfig().apiNames());
        apiNames.ifPresent(javaUserAgent::append);

        String userSuffix = trim(clientConfig.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX));
        if (!StringUtils.isEmpty(userSuffix)) {
            javaUserAgent.append(SPACE).append(userSuffix);
        }

        return javaUserAgent.toString();
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

    /**
     * This structure is used for external users as well as for internal tracking of features.
     * It's not governed by a specification.
     * Internal usage should be migrated to business metrics or another designated metadata field,
     * leaving these values to be completely user-set, in which case the result would in most cases be empty.
     * <p>
     * Currently tracking these SDK values (remove from list as they're migrated):
     * PAGINATED/sdk-version, hll/s3Multipart, hll/ddb-enh, hll/cw-mp, hll/waiter, hll/cross-region, ft/s3-transfer
     */
    private Optional<String> requestApiNames(List<ApiName> requestApiNames) {
        if (requestApiNames.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder concatenatedNames = new StringBuilder();
        requestApiNames.forEach(apiName -> concatenatedNames.append(SPACE)
                                                            .append(apiName.name())
                                                            .append(SLASH)
                                                            .append(apiName.version()));
        return Optional.of(concatenatedNames.toString());
    }
}
