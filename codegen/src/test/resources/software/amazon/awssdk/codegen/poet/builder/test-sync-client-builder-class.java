package software.amazon.awssdk.services.json;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.AwsTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;

/**
 * Internal implementation of {@link JsonClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonClientBuilder extends DefaultJsonBaseClientBuilder<JsonClientBuilder, JsonClient> implements
                                                                                                         JsonClientBuilder {
    @Override
    public DefaultJsonClientBuilder tokenProvider(AwsTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final JsonClient buildClient() {
        return new DefaultJsonClient(super.syncClientConfiguration());
    }
}
