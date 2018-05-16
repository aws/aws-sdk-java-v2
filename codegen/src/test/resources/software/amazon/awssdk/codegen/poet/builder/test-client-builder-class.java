package software.amazon.awssdk.services.json;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.config.defaults.AwsClientConfigurationDefaults;
import software.amazon.awssdk.awscore.config.defaults.ServiceBuilderConfigurationDefaults;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal base class for {@link DefaultJsonClientBuilder} and {@link DefaultJsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultJsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    private ServiceConfiguration serviceConfiguration;

    @Override
    protected final String serviceEndpointPrefix() {
        return "json-service";
    }

    @Override
    protected final AwsClientConfigurationDefaults serviceDefaults() {
        return ServiceBuilderConfigurationDefaults.builder().defaultSigner(this::defaultSigner)
                                                  .addRequestHandlerPath("software/amazon/awssdk/services/json/execution.interceptors")
                                                  .crc32FromCompressedDataEnabled(false).build();
    }

    private Signer defaultSigner() {
        return Aws4Signer.create();
    }

    @Override
    protected final String signingName() {
        return "json-service";
    }

    public B serviceConfiguration(ServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
        return thisBuilder();
    }

    protected ServiceConfiguration serviceConfiguration() {
        return serviceConfiguration;
    }

    public void setServiceConfiguration(ServiceConfiguration serviceConfiguration) {
        serviceConfiguration(serviceConfiguration);
    }

    @Override
    protected final AttributeMap serviceSpecificHttpConfig() {
        return MyServiceHttpConfig.CONFIG;
    }
}
