package software.amazon.awssdk.services.json;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;

/**
 * Internal implementation of {@link JsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonAsyncClientBuilder extends DefaultJsonBaseClientBuilder<JsonAsyncClientBuilder, JsonAsyncClient> implements
                                                                                                                        JsonAsyncClientBuilder {
    @Override
    public DefaultJsonAsyncClientBuilder tokenProvider(SdkTokenProvider tokenProvider) {
        clientConfiguration.option(AwsClientOption.TOKEN_PROVIDER, tokenProvider);
        return this;
    }

    @Override
    protected final JsonAsyncClient buildClient() {
        return new DefaultJsonAsyncClient(super.asyncClientConfiguration());
    }
}
