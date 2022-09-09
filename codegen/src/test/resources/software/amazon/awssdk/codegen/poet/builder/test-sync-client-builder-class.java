package software.amazon.awssdk.services.json;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.json.rules.JsonEndpointProvider;

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
    protected final JsonClient buildClient() {
        return new DefaultJsonClient(super.syncClientConfiguration());
    }
}
