package software.amazon.awssdk.services.json;

import java.util.ArrayList;
import java.util.List;
import software.amazon.MyServiceHttpConfig;
import software.amazon.MyServiceRetryPolicy;
import software.amazon.MyServiceRetryStrategy;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.json.endpoints.JsonClientContextParams;
import software.amazon.awssdk.services.json.endpoints.JsonEndpointProvider;
import software.amazon.awssdk.services.json.endpoints.internal.JsonEndpointAuthSchemeInterceptor;
import software.amazon.awssdk.services.json.endpoints.internal.JsonRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.json.endpoints.internal.JsonResolveEndpointInterceptor;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal base class for {@link DefaultJsonClientBuilder} and {@link DefaultJsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultJsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
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
                                  .option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(SdkClientOption.SERVICE_CONFIGURATION, ServiceConfiguration.builder().build())
                                  .option(AwsClientOption.TOKEN_PROVIDER, defaultTokenProvider())
                                  .option(SdkAdvancedClientOption.TOKEN_SIGNER, defaultTokenSigner()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        List<ExecutionInterceptor> endpointInterceptors = new ArrayList<>();
        endpointInterceptors.add(new JsonResolveEndpointInterceptor());
        endpointInterceptors.add(new JsonEndpointAuthSchemeInterceptor());
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
        serviceConfigBuilder.profileFile(serviceConfigBuilder.profileFileSupplier() != null ?
                                         serviceConfigBuilder.profileFileSupplier() :
                                         config.option(SdkClientOption.PROFILE_FILE_SUPPLIER));
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
        return config.toBuilder().option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED, finalServiceConfig.dualstackEnabled())
                     .option(AwsClientOption.FIPS_ENDPOINT_ENABLED, finalServiceConfig.fipsModeEnabled())
                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                     .option(SdkClientOption.RETRY_POLICY, MyServiceRetryPolicy.resolveRetryPolicy(config))
                     .option(SdkClientOption.RETRY_STRATEGY, MyServiceRetryStrategy.resolveRetryStrategy(config))
                     .option(SdkClientOption.SERVICE_CONFIGURATION, finalServiceConfig).build();
    }

    private Signer defaultSigner() {
        return Aws4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "json-service";
    }

    private JsonEndpointProvider defaultEndpointProvider() {
        return JsonEndpointProvider.defaultProvider();
    }

    public B serviceConfiguration(ServiceConfiguration serviceConfiguration) {
        clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION, serviceConfiguration);
        return thisBuilder();
    }

    public void setServiceConfiguration(ServiceConfiguration serviceConfiguration) {
        serviceConfiguration(serviceConfiguration);
    }

    private SdkTokenProvider defaultTokenProvider() {
        return DefaultAwsTokenProvider.create();
    }

    private Signer defaultTokenSigner() {
        return BearerTokenSigner.create();
    }

    @Override
    protected final AttributeMap serviceHttpConfig() {
        AttributeMap result = MyServiceHttpConfig.defaultHttpConfig();
        return result;
    }

    protected static void validateClientOptions(SdkClientConfiguration c) {
        Validate.notNull(c.option(SdkAdvancedClientOption.SIGNER),
                         "The 'overrideConfiguration.advancedOption[SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(SdkAdvancedClientOption.TOKEN_SIGNER),
                         "The 'overrideConfiguration.advancedOption[TOKEN_SIGNER]' must be configured in the client builder.");
        Validate.notNull(c.option(AwsClientOption.TOKEN_PROVIDER),
                         "The 'overrideConfiguration.advancedOption[TOKEN_PROVIDER]' must be configured in the client builder.");
    }
}
