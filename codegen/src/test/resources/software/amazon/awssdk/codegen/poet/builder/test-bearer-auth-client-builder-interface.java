package software.amazon.awssdk.services.json;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.services.json.endpoints.JsonEndpointProvider;

/**
 * This includes configuration specific to Json Service that is supported by both {@link JsonClientBuilder} and
 * {@link JsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    /**
     * Set the {@link JsonEndpointProvider} implementation that will be used by the client to determine the endpoint for
     * each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(JsonEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the token provider to use for bearer token authorization. This is optional, if none is provided, the SDK will
     * use {@link software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider}.
     * <p>
     * If the service, or any of its operations require Bearer Token Authorization, then the SDK will default to this
     * token provider to retrieve the token to use for authorization.
     * <p>
     * This provider works in conjunction with the
     * {@code software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.TOKEN_SIGNER} set on the client. By
     * default it is {@link software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner}.
     */
    B tokenProvider(SdkTokenProvider tokenProvider);
}
