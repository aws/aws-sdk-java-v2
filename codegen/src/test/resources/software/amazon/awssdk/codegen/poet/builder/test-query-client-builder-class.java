package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.query.endpoints.QueryClientContextParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.services.query.endpoints.internal.QueryRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.query.endpoints.internal.QueryResolveEndpointInterceptor;
import software.amazon.awssdk.services.query.internal.QueryServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.query.internal.SdkClientConfigurationUtil;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultQueryClientBuilder} and {@link DefaultQueryAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultQueryBaseClientBuilder<B extends QueryBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
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
                                  .option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, defaultTokenProvider())
                                  .option(SdkAdvancedClientOption.TOKEN_SIGNER, defaultTokenSigner()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
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
        IdentityProvider<? extends TokenIdentity> identityProvider = config.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER);
        if (identityProvider != null) {
            IdentityProviders identityProviders = config.option(SdkClientOption.IDENTITY_PROVIDERS);
            builder.option(SdkClientOption.IDENTITY_PROVIDERS, identityProviders.toBuilder()
                                                                                .putIdentityProvider(identityProvider).build());
        }
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors).option(SdkClientOption.CLIENT_CONTEXT_PARAMS,
                                                                                    clientContextParams.build());
        return builder.build();
    }

    private Signer defaultSigner() {
        return Aws4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "query-service";
    }

    private QueryEndpointProvider defaultEndpointProvider() {
        return QueryEndpointProvider.defaultProvider();
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

    private Signer defaultTokenSigner() {
        return BearerTokenSigner.create();
    }

    @Override
    protected SdkClientConfiguration setOverrides(SdkClientConfiguration configuration) {
        ClientOverrideConfiguration overrideConfiguration = overrideConfiguration();
        if (overrideConfiguration == null) {
            return configuration;
        }
        return SdkClientConfigurationUtil.copyOverridesToConfiguration(overrideConfiguration, configuration.toBuilder()).build();
    }

    @Override
    protected SdkClientConfiguration invokePlugins(SdkClientConfiguration config) {
        List<SdkPlugin> externalPlugins = plugins();
        List<SdkPlugin> sdkPlugins = getSdkPlugins();
        if (externalPlugins.isEmpty() && sdkPlugins.isEmpty()) {
            return config;
        }
        List<SdkPlugin> plugins = CollectionUtils.mergeLists(sdkPlugins, externalPlugins);
        QueryServiceClientConfigurationBuilder.BuilderInternal serviceConfigBuilder = QueryServiceClientConfigurationBuilder
            .builder(config.toBuilder());
        serviceConfigBuilder.overrideConfiguration(overrideConfiguration());
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        overrideConfiguration(serviceConfigBuilder.overrideConfiguration());
        return serviceConfigBuilder.buildSdkClientConfiguration();
    }

    protected List<SdkPlugin> getSdkPlugins() {
        List<SdkPlugin> sdkPlugins = new ArrayList<>();
        return sdkPlugins;
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(SdkAdvancedClientOption.SIGNER),
                         "The 'overrideConfiguration.advancedOption[SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(SdkAdvancedClientOption.TOKEN_SIGNER),
                         "The 'overrideConfiguration.advancedOption[TOKEN_SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER),
                         "The 'tokenProvider' must be configured in the client builder.");
    }
}
