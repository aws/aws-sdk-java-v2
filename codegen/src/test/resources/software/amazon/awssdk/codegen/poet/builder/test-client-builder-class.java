package software.amazon.awssdk.services.json;

import java.net.URI;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.StaticSignerProvider;
import software.amazon.awssdk.client.builder.ClientBuilder;
import software.amazon.awssdk.client.builder.DefaultClientBuilder;
import software.amazon.awssdk.config.defaults.ClientConfigurationDefaults;
import software.amazon.awssdk.config.defaults.ServiceBuilderConfigurationDefaults;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal base class for {@link DefaultJsonClientBuilder} and {@link DefaultJsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
abstract class DefaultJsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends DefaultClientBuilder<B, C>
        implements ClientBuilder<B, C> {
    @Override
    protected final String serviceEndpointPrefix() {
        return "json-service";
    }

    @Override
    protected final ClientConfigurationDefaults serviceDefaults() {
        return ServiceBuilderConfigurationDefaults.builder().defaultSignerProvider(this::defaultSignerProvider)
                .addRequestHandlerPath("/software/amazon/awssdk/services/json/request.handler2s")
                .defaultEndpoint(this::defaultEndpoint).build();
    }

    private SignerProvider defaultSignerProvider() {
        Aws4Signer signer = new Aws4Signer();
        signer.setServiceName("json-service");
        signer.setRegionName(signingRegion().value());
        return new StaticSignerProvider(signer);
    }

    private URI defaultEndpoint() {
        return null;
    }

    @Override
    protected final AttributeMap serviceSpecificHttpConfig() {
        return MyServiceHttpConfig.CONFIG;
    }
}
