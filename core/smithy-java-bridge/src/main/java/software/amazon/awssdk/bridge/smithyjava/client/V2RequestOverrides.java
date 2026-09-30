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

package software.amazon.awssdk.bridge.smithyjava.client;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.bridge.smithyjava.auth.V2IdentityResolver;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.smithy.java.client.core.RequestOverrideConfig;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.java.io.uri.SmithyUri;

/**
 * Translates a v2 request's {@code overrideConfiguration()} into smithy-java's per-call
 * {@link RequestOverrideConfig} (ledger 2.3).
 *
 * <p>smithy-java has the right shape for this — {@code Client#call} takes a per-call override that can
 * replace identity resolvers and the endpoint resolver, add interceptors, and carry context — so each v2
 * field goes to whichever bridged component already owns the behavior, rather than to a parallel
 * implementation. The request's configuration itself travels in the call context under {@link #KEY}, and
 * the components read what they need from there:
 *
 * <table>
 *   <caption>Where each request-level field is honored</caption>
 *   <tr><th>field</th><th>by</th></tr>
 *   <tr><td>{@code credentialsProvider}</td><td>a per-call {@link V2IdentityResolver}, here</td></tr>
 *   <tr><td>{@code plugins}</td><td>the stock per-request configuration update, and a per-call endpoint
 *       resolver and identity resolver built from its result, here</td></tr>
 *   <tr><td>{@code headers}, {@code rawQueryParameters}, {@code apiNames}</td><td>{@link Http}, before signing</td></tr>
 *   <tr><td>{@code endpointProvider}, {@code authSchemeProvider}</td><td>{@code V2EndpointResolverBridge}</td></tr>
 *   <tr><td>{@code executionAttributes}</td><td>{@code V2EndpointResolverBridge} and {@code V2InterceptorBridge}</td></tr>
 *   <tr><td>{@code signer}</td><td>{@code V2SignerBridge}</td></tr>
 *   <tr><td>{@code apiCallTimeout}, {@code apiCallAttemptTimeout}</td><td>{@code V2Timeouts}</td></tr>
 *   <tr><td>{@code metricPublishers}</td><td>generated client code, as before</td></tr>
 * </table>
 *
 * <p>A request with no override configuration and no streaming body costs one {@code Optional} check and
 * produces no per-call config at all.
 */
@SdkProtectedApi
public final class V2RequestOverrides {

    /** The call's v2 request override configuration, for the components that honor its fields. */
    public static final Context.Key<RequestOverrideConfiguration> KEY = Context.key("v2 request override configuration");

    private final SdkClientConfiguration clientConfiguration;
    private final Function<SdkClientConfiguration, EndpointResolver> endpointResolverFactory;
    private final Function<SdkRequest, SdkClientConfiguration> configurationUpdater;

    V2RequestOverrides(SdkClientConfiguration clientConfiguration,
                       Function<SdkClientConfiguration, EndpointResolver> endpointResolverFactory,
                       Function<SdkRequest, SdkClientConfiguration> configurationUpdater) {
        this.clientConfiguration = clientConfiguration;
        this.endpointResolverFactory = endpointResolverFactory;
        this.configurationUpdater = configurationUpdater;
    }

    /**
     * The per-call config for {@code input}: {@code existing} plus whatever its request overrides require.
     *
     * @param input   the call's input.
     * @param perCall the caller's own per-call contribution (streaming bodies), or null.
     * @return the config to pass to {@code Client#call}, or null when there is nothing per-call at all.
     */
    RequestOverrideConfig apply(SerializableStruct input, Consumer<RequestOverrideConfig.Builder> perCall) {
        RequestOverrideConfiguration overrides = input instanceof SdkRequest request
                                                 ? request.overrideConfiguration().orElse(null)
                                                 : null;
        if (overrides == null && perCall == null) {
            return null;
        }
        // One builder for every per-call contributor, built once. Not RequestOverrideConfig.toBuilder(): in
        // smithy-java 1.6.1 it drops the context, and with it a streaming body (ledger 16.8).
        RequestOverrideConfig.Builder builder = RequestOverrideConfig.builder();
        if (perCall != null) {
            perCall.accept(builder);
        }
        if (overrides == null) {
            return builder.build();
        }
        SdkRequest request = (SdkRequest) input;
        builder.putConfig(KEY, overrides);

        IdentityProvider<? extends AwsCredentialsIdentity> credentials = null;
        if (!overrides.plugins().isEmpty() && configurationUpdater != null) {
            // Stock v2 runs request-level plugins against a copy of the client configuration and executes
            // the request with the result. The generated method that does that is handed to the bridge, so
            // the plugins see exactly what they see on stock; what changes is how the result is used. The
            // parts of it that the smithy pipeline reads per call -- endpoint resolution (region, endpoint
            // provider, endpoint builtins) and identity -- are rebuilt for this call from the new
            // configuration. Parts smithy reads only at client construction (transport, retry strategy)
            // are not; ledger 2.2 records the line.
            SdkClientConfiguration updated = configurationUpdater.apply(request);
            if (updated != clientConfiguration) {
                EndpointResolver endpointResolver = endpointResolverFactory.apply(updated);
                if (endpointResolver != null) {
                    builder.endpointResolver(endpointResolver);
                }
                IdentityProvider<? extends AwsCredentialsIdentity> updatedCredentials =
                    updated.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
                if (updatedCredentials != clientConfiguration.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)) {
                    credentials = updatedCredentials;
                }
            }
        }
        if (overrides instanceof AwsRequestOverrideConfiguration aws && aws.credentialsIdentityProvider().isPresent()) {
            credentials = aws.credentialsIdentityProvider().get();
        }
        if (credentials != null) {
            // Replace, not add: the client's resolver is for the same identity type and would otherwise be
            // found first.
            builder.identityResolvers(List.of(new V2IdentityResolver(credentials)));
        }
        return builder.build();
    }

    /**
     * The request's headers, raw query parameters and API names, applied to the HTTP request before
     * signing, as v2's {@code MergeCustomHeadersStage}, {@code MergeCustomQueryParamsStage} and
     * {@code ApplyUserAgentStage} apply them.
     *
     * <p>Installed after the interceptor bridge, because v2 runs those stages after interceptors'
     * {@code modifyHttpRequest}, so a request-level header replaces one an interceptor set rather than the
     * other way round.
     */
    @SdkProtectedApi
    public static final class Http implements ClientInterceptor {
        private final Map<String, List<String>> clientHeaders;
        private final String userAgentPrefix;
        private final String userAgentSuffix;

        /**
         * @param clientHeaders   the client's {@code overrideConfiguration().headers()}, merged under a request's own.
         * @param userAgentPrefix {@code SdkAdvancedClientOption.USER_AGENT_PREFIX}, or null.
         * @param userAgentSuffix {@code SdkAdvancedClientOption.USER_AGENT_SUFFIX}, or null.
         */
        public Http(Map<String, List<String>> clientHeaders, String userAgentPrefix, String userAgentSuffix) {
            this.clientHeaders = clientHeaders == null ? Map.of() : clientHeaders;
            this.userAgentPrefix = userAgentPrefix;
            this.userAgentSuffix = userAgentSuffix;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
            RequestOverrideConfiguration overrides = hook.context().get(KEY);
            boolean requestLevel = overrides != null
                                   && (!overrides.headers().isEmpty() || !overrides.rawQueryParameters().isEmpty());
            if (!(hook.request() instanceof HttpRequest request) || (!requestLevel && clientHeaders.isEmpty())) {
                return hook.request();
            }
            ModifiableHttpRequest modifiable = request.toModifiableCopy();
            // MergeCustomHeadersStage: client headers, then the request's, each appended -- so a header set at
            // both levels carries both values, the client's first -- except a single-valued header, which is
            // replaced.
            mergeHeaders(modifiable, clientHeaders);
            if (overrides != null) {
                mergeHeaders(modifiable, overrides.headers());
                if (!overrides.rawQueryParameters().isEmpty()) {
                    modifiable.setUri(withQuery(request.uri(), overrides.rawQueryParameters()));
                }
            }
            return (RequestT) modifiable;
        }

        private static void mergeHeaders(ModifiableHttpRequest request, Map<String, List<String>> headers) {
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                if (software.amazon.awssdk.utils.http.SdkHttpUtils.isSingleHeader(header.getKey())) {
                    request.setHeader(header.getKey(), header.getValue());
                } else {
                    request.addHeader(header.getKey(), header.getValue());
                }
            }
        }

        /**
         * The user agent: prefix, smithy's agent, the request's API names, suffix -- as v2's
         * {@code ApplyUserAgentStage} orders them. After signing, because smithy-java sets its
         * {@code User-Agent} late, and safe there because the user agent is not a signed header.
         */
        @Override
        @SuppressWarnings("unchecked")
        public <RequestT> RequestT modifyBeforeTransmit(RequestHook<?, ?, RequestT> hook) {
            RequestOverrideConfiguration overrides = hook.context().get(KEY);
            boolean apiNames = overrides != null && !overrides.apiNames().isEmpty();
            if (!(hook.request() instanceof HttpRequest request)
                || (!apiNames && userAgentPrefix == null && userAgentSuffix == null)) {
                return hook.request();
            }
            StringBuilder userAgent = new StringBuilder();
            if (userAgentPrefix != null) {
                userAgent.append(userAgentPrefix);
            }
            String base = request.headers().firstValue("user-agent");
            if (base != null) {
                userAgent.append(userAgent.length() > 0 ? " " : "").append(base);
            }
            if (apiNames) {
                for (ApiName apiName : overrides.apiNames()) {
                    userAgent.append(userAgent.length() > 0 ? " " : "").append(apiName.name()).append('/')
                             .append(apiName.version());
                }
            }
            if (userAgentSuffix != null) {
                userAgent.append(userAgent.length() > 0 ? " " : "").append(userAgentSuffix);
            }
            ModifiableHttpRequest modifiable = request.toModifiableCopy();
            modifiable.setHeader("user-agent", userAgent.toString());
            return (RequestT) modifiable;
        }

        private static java.net.URI withQuery(SmithyUri uri, Map<String, List<String>> params) {
            java.net.URI base = uri.toURI();
            StringBuilder query = new StringBuilder(base.getRawQuery() == null ? "" : base.getRawQuery());
            for (Map.Entry<String, List<String>> param : params.entrySet()) {
                for (String value : param.getValue()) {
                    if (query.length() > 0) {
                        query.append('&');
                    }
                    query.append(software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode(param.getKey()));
                    if (value != null) {
                        query.append('=').append(software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode(value));
                    }
                }
            }
            String path = base.getRawPath() == null ? "" : base.getRawPath();
            String prefix = base.getScheme() == null ? "" : base.getScheme() + "://" + base.getRawAuthority();
            return java.net.URI.create(prefix + path + "?" + query);
        }
    }
}
