package software.amazon.awssdk.services.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.MyServiceHttpConfig;
import software.amazon.MyServiceRetryPolicy;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.poet.plugins.TestPlugin1;
import software.amazon.awssdk.codegen.poet.plugins.TestPlugin2;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.json.auth.scheme.JsonAuthSchemeProvider;
import software.amazon.awssdk.services.json.auth.scheme.internal.JsonAuthSchemeInterceptor;
import software.amazon.awssdk.services.json.endpoints.JsonClientContextParams;
import software.amazon.awssdk.services.json.endpoints.JsonEndpointProvider;
import software.amazon.awssdk.services.json.endpoints.internal.JsonRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.json.endpoints.internal.JsonResolveEndpointInterceptor;
import software.amazon.awssdk.services.json.internal.JsonServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.json.internal.SdkClientConfigurationUtil;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultJsonClientBuilder} and {@link DefaultJsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultJsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    private final Map<String, AuthScheme<?>> additionalAuthSchemes = new HashMap<>();

    @Override
    protected final String serviceEndpointPrefix() {
        return "json-service-endpoint";
    }

    @Override
    protected final String serviceName() {
        return "Json";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                                  .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
                                  .option(SdkClientOption.AUTH_SCHEMES, authSchemes())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(SdkClientOption.SERVICE_CONFIGURATION, ServiceConfiguration.builder().build())
                                  .option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, defaultTokenProvider()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new JsonAuthSchemeInterceptor());
        endpointInterceptors.add(new JsonResolveEndpointInterceptor());
        endpointInterceptors.add(new JsonRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
            .getInterceptors("software/amazon/awssdk/services/json/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        ServiceConfiguration.Builder serviceConfigBuilder = ((ServiceConfiguration) config
            .option(SdkClientOption.SERVICE_CONFIGURATION)).toBuilder();
        serviceConfigBuilder.profileFile(serviceConfigBuilder.profileFileSupplier() != null ? serviceConfigBuilder
            .profileFileSupplier() : config.option(SdkClientOption.PROFILE_FILE_SUPPLIER));
        serviceConfigBuilder.profileName(serviceConfigBuilder.profileName() != null ? serviceConfigBuilder.profileName() : config
            .option(SdkClientOption.PROFILE_NAME));
        if (serviceConfigBuilder.dualstackEnabled() != null) {
            Validate.validState(
                config.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED) == null,
                "Dualstack has been configured on both ServiceConfiguration and the client/global level. Please limit dualstack configuration to one location.");
        } else {
            serviceConfigBuilder.dualstackEnabled(config.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED));
        }
        if (serviceConfigBuilder.fipsModeEnabled() != null) {
            Validate.validState(
                config.option(AwsClientOption.FIPS_ENDPOINT_ENABLED) == null,
                "Fips has been configured on both ServiceConfiguration and the client/global level. Please limit fips configuration to one location.");
        } else {
            serviceConfigBuilder.fipsModeEnabled(config.option(AwsClientOption.FIPS_ENDPOINT_ENABLED));
        }
        if (serviceConfigBuilder.useArnRegionEnabled() != null) {
            Validate.validState(
                clientContextParams.get(JsonClientContextParams.USE_ARN_REGION) == null,
                "UseArnRegion has been configured on both ServiceConfiguration and the client/global level. Please limit UseArnRegion configuration to one location.");
        } else {
            serviceConfigBuilder.useArnRegionEnabled(clientContextParams.get(JsonClientContextParams.USE_ARN_REGION));
        }
        if (serviceConfigBuilder.multiRegionEnabled() != null) {
            Validate.validState(
                clientContextParams.get(JsonClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS) == null,
                "DisableMultiRegionAccessPoints has been configured on both ServiceConfiguration and the client/global level. Please limit DisableMultiRegionAccessPoints configuration to one location.");
        } else if (clientContextParams.get(JsonClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS) != null) {
            serviceConfigBuilder.multiRegionEnabled(!clientContextParams
                .get(JsonClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS));
        }
        if (serviceConfigBuilder.pathStyleAccessEnabled() != null) {
            Validate.validState(
                clientContextParams.get(JsonClientContextParams.FORCE_PATH_STYLE) == null,
                "ForcePathStyle has been configured on both ServiceConfiguration and the client/global level. Please limit ForcePathStyle configuration to one location.");
        } else {
            serviceConfigBuilder.pathStyleAccessEnabled(clientContextParams.get(JsonClientContextParams.FORCE_PATH_STYLE));
        }
        if (serviceConfigBuilder.accelerateModeEnabled() != null) {
            Validate.validState(
                clientContextParams.get(JsonClientContextParams.ACCELERATE) == null,
                "Accelerate has been configured on both ServiceConfiguration and the client/global level. Please limit Accelerate configuration to one location.");
        } else {
            serviceConfigBuilder.accelerateModeEnabled(clientContextParams.get(JsonClientContextParams.ACCELERATE));
        }
        ServiceConfiguration finalServiceConfig = serviceConfigBuilder.build();
        clientContextParams.put(JsonClientContextParams.USE_ARN_REGION, finalServiceConfig.useArnRegionEnabled());
        clientContextParams.put(JsonClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS,
                                !finalServiceConfig.multiRegionEnabled());
        clientContextParams.put(JsonClientContextParams.FORCE_PATH_STYLE, finalServiceConfig.pathStyleAccessEnabled());
        clientContextParams.put(JsonClientContextParams.ACCELERATE, finalServiceConfig.accelerateModeEnabled());
        SdkClientConfiguration.Builder builder = config.toBuilder();
        IdentityProvider<? extends TokenIdentity> identityProvider = config.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER);
        if (identityProvider != null) {
            IdentityProviders identityProviders = config.option(SdkClientOption.IDENTITY_PROVIDERS);
            builder.option(SdkClientOption.IDENTITY_PROVIDERS, identityProviders.toBuilder()
                                                                                .putIdentityProvider(identityProvider).build());
        }
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
               .option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED, finalServiceConfig.dualstackEnabled())
               .option(AwsClientOption.FIPS_ENDPOINT_ENABLED, finalServiceConfig.fipsModeEnabled())
               .option(SdkClientOption.RETRY_POLICY, MyServiceRetryPolicy.resolveRetryPolicy(config))
               .option(SdkClientOption.SERVICE_CONFIGURATION, finalServiceConfig);
        return builder.build();
    }

    @Override
    protected final String signingName() {
        return "json-service";
    }

    private JsonEndpointProvider defaultEndpointProvider() {
        return JsonEndpointProvider.defaultProvider();
    }

    public B authSchemeProvider(JsonAuthSchemeProvider authSchemeProvider) {
        clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return thisBuilder();
    }

    private JsonAuthSchemeProvider defaultAuthSchemeProvider() {
        return JsonAuthSchemeProvider.defaultProvider();
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
        return Collections.unmodifiableMap(schemes);
    }

    public B serviceConfiguration(ServiceConfiguration serviceConfiguration) {
        clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION, serviceConfiguration);
        return thisBuilder();
    }

    public void setServiceConfiguration(ServiceConfiguration serviceConfiguration) {
        serviceConfiguration(serviceConfiguration);
    }

    private IdentityProvider<? extends TokenIdentity> defaultTokenProvider() {
        return DefaultAwsTokenProvider.create();
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
    protected final AttributeMap serviceHttpConfig() {
        AttributeMap result = MyServiceHttpConfig.defaultHttpConfig();
        return result;
    }

    @Override
    protected SdkClientConfiguration invokePlugins(SdkClientConfiguration config) {
        List<SdkPlugin> externalPlugins = plugins();
        List<SdkPlugin> sdkPlugins = getSdkPlugins();
        if (externalPlugins.isEmpty() && sdkPlugins.isEmpty()) {
            return config;
        }
        List<SdkPlugin> plugins = CollectionUtils.mergeLists(sdkPlugins, externalPlugins);
        JsonServiceClientConfigurationBuilder.BuilderInternal serviceConfigBuilder = JsonServiceClientConfigurationBuilder
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
        sdkPlugins.add(new TestPlugin1());
        sdkPlugins.add(new TestPlugin2());
        return sdkPlugins;
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER),
                         "The 'tokenProvider' must be configured in the client builder.");
    }
}
