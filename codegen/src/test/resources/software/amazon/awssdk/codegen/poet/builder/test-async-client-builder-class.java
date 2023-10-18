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
 * Internal implementation of {@link JsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonAsyncClientBuilder extends DefaultJsonBaseClientBuilder<JsonAsyncClientBuilder, JsonAsyncClient> implements
        JsonAsyncClientBuilder {
    @Override
    public DefaultJsonAsyncClientBuilder endpointProvider(JsonEndpointProvider endpointProvider) {
        clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    @Override
    public DefaultJsonAsyncClientBuilder tokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_IDENTITY_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final JsonAsyncClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.asyncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        JsonServiceClientConfiguration serviceClientConfiguration = initializeServiceClientConfig(clientConfiguration);
        JsonAsyncClient client = new DefaultJsonAsyncClient(serviceClientConfiguration, clientConfiguration);
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
