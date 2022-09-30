package software.amazon.awssdk.services.json;

import java.util.ArrayList;
import java.util.List;
import software.amazon.MyServiceHttpConfig;
import software.amazon.MyServiceRetryPolicy;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.json.rules.JsonEndpointProvider;
import software.amazon.awssdk.services.json.rules.internal.JsonEndpointAuthSchemeInterceptor;
import software.amazon.awssdk.services.json.rules.internal.JsonRequestSetEndpointInterceptor;
import software.amazon.awssdk.services.json.rules.internal.JsonResolveEndpointInterceptor;
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
        return config.merge(c -> c.option(SdkAdvancedClientOption.SIGNER, defaultSigner())
                                  .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                  .option(SdkClientOption.ENDPOINT_PROVIDER, defaultEndpointProvider())
                                  .option(SdkClientOption.SERVICE_CONFIGURATION, ServiceConfiguration.builder().build()));
    }

    @Override
    protected final SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
        ClasspathInterceptorChainFactory interceptorFactory = new ClasspathInterceptorChainFactory();
        List<ExecutionInterceptor> interceptors = interceptorFactory
            .getInterceptors("software/amazon/awssdk/services/json/execution.interceptors");
        List<ExecutionInterceptor> additionalInterceptors = new ArrayList<>();
        additionalInterceptors.add(new JsonResolveEndpointInterceptor());
        additionalInterceptors.add(new JsonEndpointAuthSchemeInterceptor());
        additionalInterceptors.add(new JsonRequestSetEndpointInterceptor());
        interceptors = CollectionUtils.mergeLists(interceptors, additionalInterceptors);
        interceptors = CollectionUtils.mergeLists(interceptors, config.option(SdkClientOption.EXECUTION_INTERCEPTORS));
        ServiceConfiguration.Builder c = ((ServiceConfiguration) config.option(SdkClientOption.SERVICE_CONFIGURATION))
            .toBuilder();
        c.profileFile(c.profileFile() != null ? c.profileFile() : config.option(SdkClientOption.PROFILE_FILE));
        c.profileName(c.profileName() != null ? c.profileName() : config.option(SdkClientOption.PROFILE_NAME));
        if (c.dualstackEnabled() != null) {
            Validate.validState(
                config.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED) == null,
                "Dualstack has been configured on both ServiceConfiguration and the client/global level. Please limit dualstack configuration to one location.");
        } else {
            c.dualstackEnabled(config.option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED));
        }
        if (c.fipsModeEnabled() != null) {
            Validate.validState(
                config.option(AwsClientOption.FIPS_ENDPOINT_ENABLED) == null,
                "Fips has been configured on both ServiceConfiguration and the client/global level. Please limit fips configuration to one location.");
        } else {
            c.fipsModeEnabled(config.option(AwsClientOption.FIPS_ENDPOINT_ENABLED));
        }
        return config.toBuilder().option(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED, c.dualstackEnabled())
                     .option(AwsClientOption.FIPS_ENDPOINT_ENABLED, c.fipsModeEnabled())
                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, interceptors)
                     .option(SdkClientOption.RETRY_POLICY, MyServiceRetryPolicy.resolveRetryPolicy(config))
                     .option(SdkClientOption.SERVICE_CONFIGURATION, c.build()).build();
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

    @Override
    protected final AttributeMap serviceHttpConfig() {
        AttributeMap result = MyServiceHttpConfig.defaultHttpConfig();
        return result;
    }
}
