package software.amazon.awssdk.services.json;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.json.auth.scheme.JsonAuthSchemeProvider;
import software.amazon.awssdk.services.json.endpoints.JsonEndpointProvider;

/**
 * This includes configuration specific to Json Service that is supported by both {@link JsonClientBuilder} and
 * {@link JsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface JsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    B serviceConfiguration(ServiceConfiguration serviceConfiguration);

    default B serviceConfiguration(Consumer<ServiceConfiguration.Builder> serviceConfiguration) {
        return serviceConfiguration(ServiceConfiguration.builder().applyMutation(serviceConfiguration).build());
    }

    /**
     * Set the {@link JsonEndpointProvider} implementation that will be used by the client to determine the endpoint for
     * each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(JsonEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link JsonAuthSchemeProvider} implementation that will be used by the client to resolve the auth scheme
     * for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B authSchemeProvider(JsonAuthSchemeProvider authSchemeProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Enables this client to use Custom Parameter
     */
    B customParameter(Boolean customParameter);

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
    default B tokenProvider(SdkTokenProvider tokenProvider) {
        return tokenProvider((IdentityProvider<? extends TokenIdentity>) tokenProvider);
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
    default B tokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures the client behavior for request checksum calculation.
     */
    default B requestChecksumCalculation(RequestChecksumCalculation requestChecksumCalculation) {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures the client behavior for response checksum validation.
     */
    default B responseChecksumValidation(ResponseChecksumValidation responseChecksumValidation) {
        throw new UnsupportedOperationException();
    }
}
