package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.TokenUtils;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointModeResolver;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.auth.scheme.internal.QueryAuthSchemeInterceptor;
import software.amazon.awssdk.services.query.endpoints.QueryClientContextParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.services.query.endpoints.internal.QueryRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.query.endpoints.internal.QueryResolveEndpointInterceptor;
import software.amazon.awssdk.services.query.internal.QueryServiceClientConfigurationBuilder;
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
        return config.merge(c -> c
            .option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
            .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
            .option(SdkClientOption.AUTH_SCHEMES, authSchemes())
            .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
            .lazyOption(AwsClientOption.TOKEN_PROVIDER,
                        p -> TokenUtils.toSdkTokenProvider(p.get(AwsClientOption.TOKEN_IDENTITY_PROVIDER)))
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
        SdkClientConfiguration.Builder builder = config.toBuilder();
        builder.lazyOption(SdkClientOption.IDENTITY_PROVIDERS, c -> {
            IdentityProviders.Builder result = IdentityProviders.builder();
            IdentityProvider<?> tokenIdentityProvider = c.get(AwsClientOption.TOKEN_IDENTITY_PROVIDER);
            if (tokenIdentityProvider != null) {
                result.putIdentityProvider(tokenIdentityProvider);
            }
            IdentityProvider<?> credentialsIdentityProvider = c.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
            if (credentialsIdentityProvider != null) {
                result.putIdentityProvider(credentialsIdentityProvider);
            }
            return result.build();
        });
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors);
        builder.option(SdkClientOption.CLIENT_CONTEXT_PARAMS, clientContextParams.build());
        builder.option(AwsClientOption.ACCOUNT_ID_ENDPOINT_MODE, resolveAccountIdEndpointMode(config));
        builder.lazyOptionIfAbsent(
            SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
            c -> AwsClientEndpointProvider
                .builder()
                .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_QUERY_SERVICE")
                .serviceEndpointOverrideSystemProperty("aws.endpointUrlQuery")
                .serviceProfileProperty("query_service")
                .serviceEndpointPrefix(serviceEndpointPrefix())
                .defaultProtocol("https")
                .region(c.get(AwsClientOption.AWS_REGION))
                .profileFile(c.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                .profileName(c.get(SdkClientOption.PROFILE_NAME))
                .putAdvancedOption(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT,
                                   c.get(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT))
                .dualstackEnabled(c.get(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED))
                .fipsEnabled(c.get(AwsClientOption.FIPS_ENDPOINT_ENABLED)).build());
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

    private Map<String, AuthScheme<?>> authSchemes() {
        Map<String, AuthScheme<?>> schemes = new HashMap<>(3 + this.additionalAuthSchemes.size());
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        schemes.put(awsV4AuthScheme.schemeId(), awsV4AuthScheme);
        BearerAuthScheme bearerAuthScheme = BearerAuthScheme.create();
        schemes.put(bearerAuthScheme.schemeId(), bearerAuthScheme);
        NoAuthAuthScheme noAuthAuthScheme = NoAuthAuthScheme.create();
        schemes.put(noAuthAuthScheme.schemeId(), noAuthAuthScheme);
        schemes.putAll(this.additionalAuthSchemes);
        return schemes;
    }

    public B booleanContextParam(Boolean booleanContextParam) {
        clientContextParams.put(QueryClientContextParams.BOOLEAN_CONTEXT_PARAM, booleanContextParam);
        return thisBuilder();
    }

    public B stringContextParam(String stringContextParam) {
        clientContextParams.put(QueryClientContextParams.STRING_CONTEXT_PARAM, stringContextParam);
        return thisBuilder();
    }

    public B accountIdEndpointMode(AccountIdEndpointMode accountIdEndpointMode) {
        clientConfiguration.option(AwsClientOption.ACCOUNT_ID_ENDPOINT_MODE, accountIdEndpointMode);
        return thisBuilder();
    }

    private IdentityProvider<? extends TokenIdentity> defaultTokenProvider() {
        return DefaultAwsTokenProvider.create();
    }

    @Override
    protected SdkClientConfiguration invokePlugins(SdkClientConfiguration config) {
        List<SdkPlugin> internalPlugins = internalPlugins(config);
        List<SdkPlugin> externalPlugins = plugins();
        if (internalPlugins.isEmpty() && externalPlugins.isEmpty()) {
            return config;
        }
        List<SdkPlugin> plugins = CollectionUtils.mergeLists(internalPlugins, externalPlugins);
        SdkClientConfiguration.Builder configuration = config.toBuilder();
        QueryServiceClientConfigurationBuilder serviceConfigBuilder = new QueryServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private void updateRetryStrategyClientConfiguration(SdkClientConfiguration.Builder configuration) {
        ClientOverrideConfiguration.Builder builder = configuration.asOverrideConfigurationBuilder();
        RetryMode retryMode = builder.retryMode();
        if (retryMode != null) {
            configuration.option(SdkClientOption.RETRY_STRATEGY, AwsRetryStrategy.forRetryMode(retryMode));
        } else {
            Consumer<RetryStrategy.Builder<?, ?>> configurator = builder.retryStrategyConfigurator();
            if (configurator != null) {
                RetryStrategy.Builder<?, ?> defaultBuilder = AwsRetryStrategy.defaultRetryStrategy().toBuilder();
                configurator.accept(defaultBuilder);
                configuration.option(SdkClientOption.RETRY_STRATEGY, defaultBuilder.build());
            } else {
                RetryStrategy retryStrategy = builder.retryStrategy();
                if (retryStrategy != null) {
                    configuration.option(SdkClientOption.RETRY_STRATEGY, retryStrategy);
                }
            }
        }
        configuration.option(SdkClientOption.CONFIGURED_RETRY_MODE, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_STRATEGY, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR, null);
    }

    private List<SdkPlugin> internalPlugins(SdkClientConfiguration config) {
        return Collections.emptyList();
    }

    private AccountIdEndpointMode resolveAccountIdEndpointMode(SdkClientConfiguration config) {
        AccountIdEndpointMode configuredMode = config.option(AwsClientOption.ACCOUNT_ID_ENDPOINT_MODE);
        if (configuredMode == null) {
            configuredMode = AccountIdEndpointModeResolver.create()
                                                          .profileFile(config.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                                          .profileName(config.option(SdkClientOption.PROFILE_NAME)).defaultMode(AccountIdEndpointMode.PREFERRED)
                                                          .resolve();
        }
        return configuredMode;
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER),
                         "The 'tokenProvider' must be configured in the client builder.");
    }
}
