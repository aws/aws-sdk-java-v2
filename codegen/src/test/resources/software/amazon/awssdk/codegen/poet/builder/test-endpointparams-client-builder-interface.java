package software.amazon.awssdk.services.query;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeProvider;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;

/**
 * This includes configuration specific to Query Service that is supported by both {@link QueryClientBuilder} and
 * {@link QueryAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryBaseClientBuilder<B extends QueryBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    /**
     * Set the {@link QueryEndpointProvider} implementation that will be used by the client to determine the endpoint
     * for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(QueryEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link QueryAuthSchemeProvider} implementation that will be used by the client to resolve the auth scheme
     * for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B authSchemeProvider(QueryAuthSchemeProvider authSchemeProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * A boolean client context parameter
     */
    B booleanContextParam(Boolean booleanContextParam);

    /**
     * a string client context parameter
     */
    B stringContextParam(String stringContextParam);

    /**
     * Sets the behavior when account ID based endpoints are created. See {@link AccountIdEndpointMode} for values
     */
    B accountIdEndpointMode(AccountIdEndpointMode accountIdEndpointMode);

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
