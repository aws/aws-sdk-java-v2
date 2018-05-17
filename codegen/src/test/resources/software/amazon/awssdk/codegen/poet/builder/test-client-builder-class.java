package software.amazon.awssdk.services.json;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.config.defaults.AwsClientConfigurationDefaults;
import software.amazon.awssdk.awscore.config.defaults.ServiceBuilderConfigurationDefaults;
import software.amazon.awssdk.core.signerspi.Signer;
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
        return ServiceBuilderConfigurationDefaults.builder().defaultSigner(this::defaultSigner)
                                                  .addRequestHandlerPath("software/amazon/awssdk/services/json/execution.interceptors")
                                                  .crc32FromCompressedDataEnabled(false).build();
    }

    private Signer defaultSigner() {
        return new Aws4Signer();
    }

    @Override
    protected final String signingName() {
        return "json-service";
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
