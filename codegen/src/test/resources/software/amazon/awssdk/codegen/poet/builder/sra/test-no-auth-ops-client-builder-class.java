package software.amazon.awssdk.services.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.internal.ConfigurationUpdater;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeProvider;
import software.amazon.awssdk.services.database.auth.scheme.internal.DatabaseAuthSchemeInterceptor;
import software.amazon.awssdk.services.database.endpoints.DatabaseEndpointProvider;
import software.amazon.awssdk.services.database.endpoints.internal.DatabaseRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.database.endpoints.internal.DatabaseResolveEndpointInterceptor;
import software.amazon.awssdk.services.database.internal.DatabaseServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.database.internal.SdkClientConfigurationUtil;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal base class for {@link DefaultDatabaseClientBuilder} and {@link DefaultDatabaseAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultDatabaseBaseClientBuilder<B extends DatabaseBaseClientBuilder<B, C>, C> extends
                                                                                              AwsDefaultClientBuilder<B, C> {
    private final Map<String, AuthScheme<?>> additionalAuthSchemes = new HashMap<>();

    @Override
    protected final String serviceEndpointPrefix() {
        return "database-service-endpoint";
    }

    @Override
    protected final String serviceName() {
        return "Database";
    }

    @Override
    protected final SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
        return config.merge(c -> c.option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                .option(SdkClientOption.AUTH_SCHEME_PROVIDER, defaultAuthSchemeProvider())
                .option(SdkClientOption.AUTH_SCHEMES, authSchemes())
                .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new DatabaseAuthSchemeInterceptor());
        endpointInterceptors.add(new DatabaseResolveEndpointInterceptor());
        endpointInterceptors.add(new DatabaseRequestSetEndpointInterceptor());
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
            .getInterceptors("software/amazon/awssdk/services/database/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        interceptors = CollectionUtils.mergeLists(endpointInterceptors, interceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        SdkClientConfiguration.Builder builder = config.toBuilder();
        builder.option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors);
        return builder.build();
    }

    @Override
    protected final String signingName() {
        return "database-service";
    }

    private DatabaseEndpointProvider defaultEndpointProvider() {
        return DatabaseEndpointProvider.defaultProvider();
    }

    public B authSchemeProvider(DatabaseAuthSchemeProvider authSchemeProvider) {
        clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return thisBuilder();
    }

    private DatabaseAuthSchemeProvider defaultAuthSchemeProvider() {
        return DatabaseAuthSchemeProvider.defaultProvider();
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

    protected ConfigurationUpdater<SdkServiceClientConfiguration.Builder> defaultConfigurationUpdater() {
        return (consumer, configBuilder) -> {
            DatabaseServiceClientConfigurationBuilder.BuilderInternal serviceConfigBuilder = DatabaseServiceClientConfigurationBuilder
                    .builder(configBuilder);
            consumer.accept(serviceConfigBuilder);
            return serviceConfigBuilder.buildSdkClientConfiguration();
        };
    }

    @Override
    protected SdkClientConfiguration setOverrides(SdkClientConfiguration configuration) {
        ClientOverrideConfiguration overrideConfiguration = overrideConfiguration();
        if (overrideConfiguration == null) {
            return configuration;
        }
        return SdkClientConfigurationUtil.copyOverridesToConfiguration(overrideConfiguration, configuration.toBuilder()).build();
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
    }
}
