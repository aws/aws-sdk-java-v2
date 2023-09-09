package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.auth.BearerAuthScheme;
import software.amazon.awssdk.http.auth.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.protocols.query.interceptor.QueryParametersToBodyInterceptor;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.auth.scheme.internal.QueryAuthSchemeInterceptor;
import software.amazon.awssdk.services.query.endpoints.QueryClientContextParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.services.query.endpoints.internal.QueryRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.query.endpoints.internal.QueryResolveEndpointInterceptor;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultQueryClientBuilder} and {@link DefaultQueryAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultQueryBaseClientBuilder<B extends QueryBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    private final Map<String, AuthScheme<?>> additionalAuthSchemes = new HashMap<>();

    @Override
    protected final String serviceEndpointPrefix() {
        return "query-service";
    }

    @Override
    protected final String serviceName() {
        return "Query";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                                  .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
                                  .option(SdkClientOption.AUTH_SCHEMES, authSchemes())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, defaultTokenProvider()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new QueryAuthSchemeInterceptor());
        endpointInterceptors.add(new QueryResolveEndpointInterceptor());
        endpointInterceptors.add(new QueryRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
            .getInterceptors("software/amazon/awssdk/services/query/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        List<ExecutionInterceptor> protocolInterceptors = Collections.singletonList(new QueryParametersToBodyInterceptor());
        interceptors = CollectionUtils.mergeLists(interceptors, protocolInterceptors);
        SdkClientConfiguration.Builder builder = config.toBuilder();
        IdentityProvider<? extends TokenIdentity> identityProvider = config.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER);
        if (identityProvider != null) {
            IdentityProviderConfiguration identityProviderConfig = config.option(SdkClientOption.IDENTITY_PROVIDER_CONFIGURATION);
            builder.option(SdkClientOption.IDENTITY_PROVIDER_CONFIGURATION, identityProviderConfig.toBuilder()
                                                                                                  .putIdentityProvider(identityProvider).build());
        }
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors).option(SdkClientOption.CLIENT_CONTEXT_PARAMS,
                                                                                    clientContextParams.build());
        return builder.build();
    }

    @Override
    protected final String signingName() {
        return "query-service";
    }

    private QueryEndpointProvider defaultEndpointProvider() {
        return QueryEndpointProvider.defaultProvider();
    }

    public B authSchemeProvider(QueryAuthSchemeProvider authSchemeProvider) {
        clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return thisBuilder();
    }

    private QueryAuthSchemeProvider defaultAuthSchemeProvider() {
        return QueryAuthSchemeProvider.defaultProvider();
    }

    @Override
    public B putAuthScheme(AuthScheme<?> authScheme) {
        additionalAuthSchemes.put(authScheme.schemeId(), authScheme);
        return thisBuilder();
    }

    public B booleanContextParam(Boolean booleanContextParam) {
        clientContextParams.put(QueryClientContextParams.BOOLEAN_CONTEXT_PARAM, booleanContextParam);
        return thisBuilder();
    }

    public B stringContextParam(String stringContextParam) {
        clientContextParams.put(QueryClientContextParams.STRING_CONTEXT_PARAM, stringContextParam);
        return thisBuilder();
    }

    private IdentityProvider<? extends TokenIdentity> defaultTokenProvider() {
        return DefaultAwsTokenProvider.create();
    }

    private Map<String, AuthScheme<?>> authSchemes() {
        Map<String, AuthScheme<?>> schemes = new HashMap<>(3 + this.additionalAuthSchemes.size());
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        schemes.put(awsV4AuthScheme.schemeId(), awsV4AuthScheme);
        BearerAuthScheme bearerAuthScheme = BearerAuthScheme.create();
        schemes.put(bearerAuthScheme.schemeId(), bearerAuthScheme);
        NoAuthAuthScheme noAuthAuthScheme = NoAuthAuthScheme.create();
        schemes.put(noAuthAuthScheme.schemeId(), noAuthAuthScheme);
        schemes.putAll(this.additionalAuthSchemes);
        return Collections.unmodifiableMap(schemes);
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER),
                         "The 'tokenProvider' must be configured in the client builder.");
    }
}
