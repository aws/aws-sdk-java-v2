package software.amazon.awssdk.services.json;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.StaticSignerProvider;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.config.defaults.AwsClientConfigurationDefaults;
import software.amazon.awssdk.awscore.config.defaults.ServiceBuilderConfigurationDefaults;
import software.amazon.awssdk.core.runtime.auth.SignerProvider;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal base class for {@link DefaultJsonClientBuilder} and {@link DefaultJsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultJsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsDefaultClientBuilder<B, C> {
    private AdvancedConfiguration advancedConfiguration;

    @Override
    protected final String serviceEndpointPrefix() {
        return "json-service";
    }

    @Override
    protected final AwsClientConfigurationDefaults serviceDefaults() {
        return ServiceBuilderConfigurationDefaults.builder().defaultSignerProvider(this::defaultSignerProvider)
                                                  .addRequestHandlerPath("software/amazon/awssdk/services/json/execution.interceptors")
                                                  .crc32FromCompressedDataEnabled(false).build();
    }

    private SignerProvider defaultSignerProvider() {
        Aws4Signer signer = new Aws4Signer();
        signer.setServiceName("json-service");
        signer.setRegionName(signingRegion().value());
        return StaticSignerProvider.create(signer);
    }

    public B advancedConfiguration(AdvancedConfiguration advancedConfiguration) {
        this.advancedConfiguration = advancedConfiguration;
        return thisBuilder();
    }

    protected AdvancedConfiguration advancedConfiguration() {
        return advancedConfiguration;
    }

    public void setAdvancedConfiguration(AdvancedConfiguration advancedConfiguration) {
        advancedConfiguration(advancedConfiguration);
    }

    @Override
    protected final AttributeMap serviceSpecificHttpConfig() {
        return MyServiceHttpConfig.CONFIG;
    }
}
