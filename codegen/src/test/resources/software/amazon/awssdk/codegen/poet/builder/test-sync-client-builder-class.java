package software.amazon.awssdk.services.json;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
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
        return new DefaultJsonClient(clientConfiguration);
    }
}
