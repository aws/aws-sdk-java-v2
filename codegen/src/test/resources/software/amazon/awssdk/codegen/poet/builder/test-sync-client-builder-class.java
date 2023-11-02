package software.amazon.awssdk.services.json;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.json.endpoints.JsonEndpointProvider;

/**
 * Internal implementation of {@link JsonClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonClientBuilder extends DefaultJsonBaseClientBuilder<JsonClientBuilder, JsonClient> implements
        JsonClientBuilder {
    @Override
    public DefaultJsonClientBuilder endpointProvider(JsonEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public DefaultJsonClientBuilder tokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final JsonClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.syncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        JsonServiceClientConfiguration serviceClientConfiguration = initializeServiceClientConfig(clientConfiguration);
        JsonClient client = new DefaultJsonClient(serviceClientConfiguration, clientConfiguration);
        return client;
    }

    private JsonServiceClientConfiguration initializeServiceClientConfig(SdkClientConfiguration clientConfig) {
        URI endpointOverride = null;
        if (Boolean.TRUE.equals(clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfig.option(SdkClientOption.ENDPOINT);
        }
        return JsonServiceClientConfiguration.builder().overrideConfiguration(overrideConfiguration())
                .region(clientConfig.option(AwsClientOption.AWS_REGION)).endpointOverride(endpointOverride)
                .endpointProvider(clientConfig.option(SdkClientOption.ENDPOINT_PROVIDER)).build();
    }
}
