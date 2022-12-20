package software.amazon.awssdk.services.json;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
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
    public DefaultJsonAsyncClientBuilder tokenProvider(SdkTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final JsonAsyncClient buildClient() {
        SdkClientConfiguration clientConfiguration = super.asyncClientConfiguration();
        this.validateClientOptions(clientConfiguration);
        return new DefaultJsonAsyncClient(clientConfiguration);
    }
}
