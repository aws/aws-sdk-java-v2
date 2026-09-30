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

package software.amazon.awssdk.bridge.smithyjava.endpoints;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.bridge.smithyjava.auth.V2IdentityResolver;
import software.amazon.awssdk.bridge.smithyjava.auth.V2SigningAuthScheme;
import software.amazon.awssdk.bridge.smithyjava.client.V2OperationMetadata;
import software.amazon.awssdk.bridge.smithyjava.client.V2RequestOverride;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.endpoints.EndpointResolverParams;

/**
 * Implements smithy-java's {@link EndpointResolver} by delegating to the AWS SDK v2 endpoint rules
 * engine.
 *
 * <h2>Why this is bridged rather than translated</h2>
 *
 * <p>Endpoint resolution is the single largest piece of per-service behavior in the SDK: FIPS,
 * dual-stack, account-ID-based routing, {@code ResourceArn}-driven routing, client/static/operation
 * context params, endpoint overrides, and any customer-supplied {@code EndpointProvider}. The v2 rules
 * engine already implements all of it, generated from the same model. Reimplementing it against
 * smithy-java's rules runtime would be a large amount of work whose only outcome would be to arrive at
 * the same answers.
 *
 * <p>The bridge is possible without extra codegen because the generated
 * {@code <Service>ResolveEndpointInterceptor.ruleParams(SdkRequest, ExecutionAttributes)} method is
 * {@code public static} and self-contained. Generated client code passes a lambda that composes it
 * with the configured provider; see {@link V2RuleParamsResolver}.
 *
 * <h2>Account-ID routing</h2>
 *
 * <p>The rules engine reads the caller's account ID out of the {@code SELECTED_AUTH_SCHEME} execution
 * attribute. That works here for free: smithy's {@code ClientPipeline} resolves the auth scheme and
 * identity <em>before</em> resolving the endpoint, and stashes the identity in
 * {@link CallContext#IDENTITY}, so this class reads the already-resolved identity out of the call
 * context rather than resolving credentials a second time.
 *
 * <h2>Cost</h2>
 *
 * <p>Each resolution allocates an {@link ExecutionAttributes} copy, a
 * {@link BusinessMetricCollection}, a {@link SelectedAuthScheme}, and a completed future. smithy
 * resolves endpoints once per <em>attempt</em> rather than once per execution, so on a retried call
 * this happens more often than under v2. A single-entry cache keyed on the resolved URI avoids
 * re-allocating the smithy {@link Endpoint} on the steady-state path, where the answer never changes.
 */
@SdkProtectedApi
public final class V2EndpointResolverBridge implements EndpointResolver {

    /**
     * Composes the generated {@code ruleParams(...)} with the configured {@code EndpointProvider}.
     *
     * <p>Generated client code supplies this as a lambda, which is what keeps this class free of any
     * service-specific types:
     * {@snippet :
     * (request, attributes) -> provider.resolveEndpoint(
     *         DynamoDbResolveEndpointInterceptor.ruleParams(request, attributes)).join()
     * }
     */
    @FunctionalInterface
    public interface V2RuleParamsResolver {
        software.amazon.awssdk.endpoints.Endpoint resolve(SdkRequest request, ExecutionAttributes attributes);
    }

    /**
     * Resolves v2's auth-scheme options for a call with v2's own auth-scheme provider.
     *
     * <p>Generated client code supplies this, like {@link V2RuleParamsResolver}, composing the configured
     * {@code AUTH_SCHEME_PROVIDER} with the generated {@code <Service>AuthSchemeInterceptor.authSchemeParams}:
     * {@snippet :
     * (request, attributes) -> provider.resolveAuthScheme(S3AuthSchemeInterceptor.authSchemeParams(request, attributes))
     * }
     * S3's provider is endpoint-aware — it re-derives signing name, region and double-encoding from the
     * endpoint's own auth scheme — so it needs the same attributes the rules engine does, which is why it
     * runs here rather than in the auth scheme.
     */
    @FunctionalInterface
    public interface V2AuthOptionsResolver {
        List<AuthSchemeOption> resolve(SdkRequest request, ExecutionAttributes attributes);
    }

    private static final String SIGV4 = "aws.auth#sigv4";

    private final V2RuleParamsResolver v2Resolver;
    private final V2AuthOptionsResolver authOptionsResolver;
    private final ExecutionAttributes template;
    private final AuthSchemeOption sigV4Option;

    // Single-entry memo of the last resolved endpoint. Endpoints are stable for the overwhelming
    // majority of calls, so this keeps the steady-state path from re-parsing the URI.
    private volatile URI cachedUri;
    private volatile Endpoint cachedEndpoint;

    /**
     * @param v2Config   the v2 client configuration to read endpoint builtins from.
     * @param v2Resolver composition of the generated {@code ruleParams} and the endpoint provider.
     */
    public V2EndpointResolverBridge(SdkClientConfiguration v2Config, V2RuleParamsResolver v2Resolver) {
        this(v2Config, v2Resolver, null);
    }

    /**
     * @param v2Config            the v2 client configuration to read endpoint builtins from.
     * @param v2Resolver          composition of the generated {@code ruleParams} and the endpoint provider.
     * @param authOptionsResolver v2's auth-scheme provider for the signer bridge, or null when the client
     *                            signs with smithy-java's own SigV4.
     */
    public V2EndpointResolverBridge(SdkClientConfiguration v2Config, V2RuleParamsResolver v2Resolver,
                                    V2AuthOptionsResolver authOptionsResolver) {
        this.v2Resolver = v2Resolver;
        this.authOptionsResolver = authOptionsResolver;
        this.sigV4Option = AuthSchemeOption.builder().schemeId("aws.auth#sigv4").build();
        this.template = buildTemplate(v2Config);
    }

    // Everything the rules engine reads that does not vary per call. Built once at client construction
    // and copied per resolution, because ruleParams mutates BUSINESS_METRICS.
    private static ExecutionAttributes buildTemplate(SdkClientConfiguration v2Config) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, v2Config.option(AwsClientOption.AWS_REGION));
        attributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED,
                                v2Config.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED));
        attributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED,
                                v2Config.option(AwsClientOption.FIPS_ENDPOINT_ENABLED));
        attributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                v2Config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER));
        attributes.putAttribute(AwsExecutionAttribute.AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE,
                                v2Config.option(AwsClientOption.ACCOUNT_ID_ENDPOINT_MODE));
        attributes.putAttribute(SdkExecutionAttribute.SERVICE_NAME,
                                v2Config.option(SdkClientOption.SERVICE_NAME));
        attributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS,
                                v2Config.option(SdkClientOption.CLIENT_CONTEXT_PARAMS));
        // The rest are for the signer bridge, and are the attributes AwsExecutionContextBuilder puts on every
        // stock call that the auth-scheme provider and the checksum logic read: S3's endpoint-aware provider
        // resolves the endpoint itself, and the checksum decision reads the client's calculation mode.
        attributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER,
                                v2Config.option(SdkClientOption.ENDPOINT_PROVIDER));
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER,
                                v2Config.option(SdkClientOption.AUTH_SCHEME_PROVIDER));
        attributes.putAttribute(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION,
                                v2Config.option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION));
        attributes.putAttribute(SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION,
                                v2Config.option(SdkClientOption.RESPONSE_CHECKSUM_VALIDATION));
        attributes.putAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT,
                                v2Config.option(AwsClientOption.USE_GLOBAL_ENDPOINT));
        attributes.putAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION,
                                v2Config.option(software.amazon.awssdk.core.client.config.SdkAdvancedClientOption
                                                    .DISABLE_HOST_PREFIX_INJECTION));
        return attributes;
    }

    @Override
    public Endpoint resolveEndpoint(EndpointResolverParams params) {
        ExecutionAttributes attributes = template.copy();
        // A request's own endpoint provider, auth-scheme provider and execution attributes replace the
        // client's, as AwsExecutionContextBuilder resolves them on stock v2. See V2RequestOverride.
        RequestOverrideConfiguration overrides = params.context().get(V2RequestOverride.KEY);
        if (overrides != null) {
            overrides.endpointProvider()
                     .ifPresent(p -> attributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER, p));
            overrides.authSchemeProvider()
                     .ifPresent(p -> attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER, p));
            ExecutionAttributes requestAttributes = overrides.executionAttributes();
            if (requestAttributes != null) {
                requestAttributes.getAttributes().forEach((k, v) -> putRaw(attributes, k, v));
            }
        }
        attributes.putAttribute(AwsExecutionAttribute.OPERATION_NAME, params.operation().schema().id().getName());
        // Business metrics recorded by the rules engine are collected and discarded: v2 stamps them into
        // the User-Agent, and smithy-java builds its own. See compatability_issues.md 7.2.
        attributes.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, new BusinessMetricCollection());
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme(params));

        SdkRequest input = (SdkRequest) params.inputValue();
        if (authOptionsResolver != null) {
            putOperationMetadata(params, attributes);
        }

        software.amazon.awssdk.endpoints.Endpoint v2Endpoint = v2Resolver.resolve(input, attributes);
        attributes.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, v2Endpoint);

        if (authOptionsResolver != null) {
            // Endpoint resolution precedes signing in ClientPipeline, so this is where the signer's inputs
            // are assembled. They ride on the endpoint rather than in the context, because the context
            // handed to a resolver is read-only; the pipeline stores the endpoint under
            // CallContext.ENDPOINT, where the signer finds it. Per-attempt, so not cached.
            return Endpoint.builder()
                           .uri(v2Endpoint.url())
                           .putProperty(V2SigningAuthScheme.SIGNING_INPUTS,
                                        new V2SigningAuthScheme.SigningInputs(
                                            withEndpointSignerProperties(sigV4Option(input, attributes), v2Endpoint),
                                            attributes))
                           .build();
        }
        return toSmithy(v2Endpoint.url());
    }

    /**
     * Applies an operation's {@code @endpoint} host prefix to a resolved endpoint, as the stock
     * {@code <Service>ResolveEndpointInterceptor} does after resolution: skipped when the client disables host
     * prefix injection, and validated as a hostname label before it is used. Generated code composes this
     * with {@code ruleParams} and the interceptor's {@code hostPrefix(operationName, request)} (ledger 5.4).
     *
     * @param endpoint   the rules engine's answer.
     * @param hostPrefix the operation's resolved prefix, if it has one.
     * @param attributes the attempt's attributes, for {@code DISABLE_HOST_PREFIX_INJECTION}.
     * @return the endpoint, with the prefix prepended to its host when one applies.
     */
    public static software.amazon.awssdk.endpoints.Endpoint withHostPrefix(
            software.amazon.awssdk.endpoints.Endpoint endpoint, java.util.Optional<String> hostPrefix,
            ExecutionAttributes attributes) {
        if (hostPrefix.isEmpty() || hostPrefix.get().isBlank()
            || attributes.getOptionalAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION).orElse(false)) {
            return endpoint;
        }
        String prefix = hostPrefix.get();
        String label = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
        software.amazon.awssdk.utils.HostnameValidator.validateHostnameCompliant(label, "hostPrefix", "request");
        URI url = endpoint.url();
        try {
            URI prefixed = new URI(url.getScheme(), null, prefix + url.getHost(), url.getPort(), url.getPath(),
                                   url.getQuery(), url.getFragment());
            return endpoint.toBuilder().url(prefixed).build();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid host prefix " + prefix, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void putRaw(ExecutionAttributes attributes, ExecutionAttribute<?> key, Object value) {
        attributes.putAttribute((ExecutionAttribute<Object>) key, value);
    }

    @SuppressWarnings("unchecked")
    private static void putOperationMetadata(EndpointResolverParams params, ExecutionAttributes attributes) {
        if (params.operation() instanceof V2OperationMetadata<?> metadata) {
            ((V2OperationMetadata<Object>) metadata).putExecutionAttributes(params.inputValue(),
                                                                          new V2OperationMetadata.Attributes(attributes));
        }
    }

    /**
     * The endpoint's own SigV4 properties, applied over the provider's, as the stock
     * {@code <Service>ResolveEndpointInterceptor#authSchemeWithEndpointSignerProperties} applies them: an
     * endpoint rule's {@code authSchemes} decides signing name, signing region and double-encoding, whatever
     * the auth-scheme provider said. This is how S3 access points, Outposts and the like sign for the right
     * service, and it is also why a request-level {@code authSchemeProvider} does not change an S3 request's
     * signing scope on stock v2 — so it does not on the bridge either.
     */
    private static AuthSchemeOption withEndpointSignerProperties(AuthSchemeOption option,
                                                                 software.amazon.awssdk.endpoints.Endpoint endpoint) {
        List<software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme> schemes =
            endpoint.attribute(software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute.AUTH_SCHEMES);
        if (schemes == null) {
            return option;
        }
        for (software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme scheme : schemes) {
            if (scheme.schemeId().equals(option.schemeId())
                && scheme instanceof software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme v4) {
                AuthSchemeOption.Builder builder = option.toBuilder();
                if (v4.isDisableDoubleEncodingSet()) {
                    builder.putSignerProperty(software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.DOUBLE_URL_ENCODE,
                                              !v4.disableDoubleEncoding());
                }
                if (v4.signingRegion() != null) {
                    builder.putSignerProperty(software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME,
                                              v4.signingRegion());
                }
                if (v4.signingName() != null) {
                    builder.putSignerProperty(software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.SERVICE_SIGNING_NAME,
                                              v4.signingName());
                }
                return builder.build();
            }
        }
        return option;
    }

    /**
     * v2's SigV4 option for this call. The provider may offer others first — SigV4a for a multi-region
     * access point, S3 Express session auth — which the signer bridge does not implement, so they are
     * skipped rather than half-supported; ledger 4.x records it.
     */
    private AuthSchemeOption sigV4Option(SdkRequest input, ExecutionAttributes attributes) {
        for (AuthSchemeOption option : authOptionsResolver.resolve(input, attributes)) {
            if (SIGV4.equals(option.schemeId())) {
                return option;
            }
        }
        throw new IllegalStateException("v2's auth-scheme provider offered no SigV4 option for "
                                        + attributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME)
                                        + "; the signer bridge implements SigV4 only");
    }

    // Hands the already-resolved identity back to the v2 rules engine so the AccountId builtin resolves
    // without a second credential lookup. The signer is never invoked; only identity() is read.
    private SelectedAuthScheme<AwsCredentialsIdentity> selectedAuthScheme(EndpointResolverParams params) {
        var identity = params.context().get(CallContext.IDENTITY);
        AwsCredentialsIdentity v2Identity =
                identity instanceof software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity smithy
                        ? V2IdentityResolver.toV2(smithy)
                        : AwsCredentialsIdentity.builder().accessKeyId("").secretAccessKey("").build();
        return new SelectedAuthScheme<>(CompletableFuture.completedFuture(v2Identity),
                                        UnusedSigner.INSTANCE,
                                        sigV4Option);
    }

    private Endpoint toSmithy(URI url) {
        Endpoint cached = cachedEndpoint;
        if (cached != null && url.equals(cachedUri)) {
            return cached;
        }
        Endpoint resolved = Endpoint.builder().uri(url).build();
        this.cachedUri = url;
        this.cachedEndpoint = resolved;
        return resolved;
    }
}
