package software.amazon.awssdk.services.json;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.services.builder.SyncClientDecorator;
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
    public DefaultJsonClientBuilder tokenProvider(SdkTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final JsonClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.syncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        JsonServiceClientConfiguration serviceClientConfiguration = initializeServiceClientConfig(clientConfiguration);
        JsonClient client = new DefaultJsonClient(serviceClientConfiguration, clientConfiguration);
        return new SyncClientDecorator().decorate(client, clientConfiguration, clientContextParams.copy().build());
    }

    private JsonServiceClientConfiguration initializeServiceClientConfig(SdkClientConfiguration clientConfig) {
        URI endpointOverride = null;
        EndpointProvider endpointProvider = clientConfig.option(SdkClientOption.ENDPOINT_PROVIDER);
        if (clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN) != null
            && Boolean.TRUE.equals(clientConfig.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            endpointOverride = clientConfig.option(SdkClientOption.ENDPOINT);
        }
        return JsonServiceClientConfiguration.builder().overrideConfiguration(overrideConfiguration())
                                             .region(clientConfig.option(AwsClientOption.AWS_REGION)).endpointOverride(endpointOverride)
                                             .endpointProvider(endpointProvider).build();
    }
}
