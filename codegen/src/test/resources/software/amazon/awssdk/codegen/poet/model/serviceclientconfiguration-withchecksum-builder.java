package software.amazon.awssdk.services.json.internal;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.json.JsonServiceClientConfiguration;
import software.amazon.awssdk.services.json.auth.scheme.JsonAuthSchemeProvider;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class JsonServiceClientConfigurationBuilder implements JsonServiceClientConfiguration.Builder {
    private final SdkClientConfiguration.Builder config;

    public JsonServiceClientConfigurationBuilder() {
        this(SdkClientConfiguration.builder());
    }

    public JsonServiceClientConfigurationBuilder(SdkClientConfiguration.Builder config) {
        this.config = config;
    }

    /**
     * Sets the value for client override configuration
     */
    @Override
    public JsonServiceClientConfiguration.Builder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        config.putAll(overrideConfiguration);
        return this;
    }

    /**
     * Gets the value for client override configuration
     */
    @Override
    public ClientOverrideConfiguration overrideConfiguration() {
        return config.asOverrideConfigurationBuilder().build();
    }

    /**
     * Sets the value for endpoint override
     */
    @Override
    public JsonServiceClientConfiguration.Builder endpointOverride(URI endpointOverride) {
        if (endpointOverride != null) {
            config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, ClientEndpointProvider.forEndpointOverride(endpointOverride));
        } else {
            config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, null);
        }
        return this;
    }

    /**
     * Gets the value for endpoint override
     */
    @Override
    public URI endpointOverride() {
        ClientEndpointProvider clientEndpoint = config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (clientEndpoint != null && clientEndpoint.isEndpointOverridden()) {
            return clientEndpoint.clientEndpoint();
        }
        return null;
    }

    /**
     * Sets the value for endpoint provider
     */
    @Override
    public JsonServiceClientConfiguration.Builder endpointProvider(EndpointProvider endpointProvider) {
        config.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
        return this;
    }

    /**
     * Gets the value for endpoint provider
     */
    @Override
    public EndpointProvider endpointProvider() {
        return config.option(SdkClientOption.ENDPOINT_PROVIDER);
    }

    /**
     * Sets the value for AWS region
     */
    @Override
    public JsonServiceClientConfiguration.Builder region(Region region) {
        config.option(AwsClientOption.AWS_REGION, region);
        return this;
    }

    /**
     * Gets the value for AWS region
     */
    @Override
    public Region region() {
        return config.option(AwsClientOption.AWS_REGION);
    }

    /**
     * Sets the value for credentials provider
     */
    @Override
    public JsonServiceClientConfiguration.Builder credentialsProvider(
        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        config.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, credentialsProvider);
        return this;
    }

    /**
     * Gets the value for credentials provider
     */
    @Override
    public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
        return config.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
    }

    @Override
    public JsonServiceClientConfiguration.Builder putAuthScheme(AuthScheme<?> authScheme) {
        config.computeOptionIfAbsent(SdkClientOption.AUTH_SCHEMES, HashMap::new).put(authScheme.schemeId(), authScheme);
        return this;
    }

    /**
     * Gets the value for auth schemes
     */
    @Override
    public Map<String, AuthScheme<?>> authSchemes() {
        Map<String, AuthScheme<?>> authSchemes = config.option(SdkClientOption.AUTH_SCHEMES);
        return Collections.unmodifiableMap(authSchemes == null ? Collections.emptyMap() : authSchemes);
    }

    /**
     * Sets the value for auth scheme provider
     */
    @Override
    public JsonServiceClientConfiguration.Builder authSchemeProvider(JsonAuthSchemeProvider authSchemeProvider) {
        config.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return this;
    }

    /**
     * Gets the value for auth scheme provider
     */
    @Override
    public JsonAuthSchemeProvider authSchemeProvider() {
        AuthSchemeProvider result = config.option(SdkClientOption.AUTH_SCHEME_PROVIDER);
        if (result == null) {
            return null;
        }
        return Validate.isInstanceOf(JsonAuthSchemeProvider.class, result, "Expected an instance of "
                                                                           + JsonAuthSchemeProvider.class.getSimpleName());
    }

    /**
     * Sets the value for client behavior for response checksum calculation
     */
    @Override
    public JsonServiceClientConfiguration.Builder responseChecksumValidation(ResponseChecksumValidation responseChecksumValidation) {
        config.option(SdkClientOption.RESPONSE_CHECKSUM_VALIDATION, responseChecksumValidation);
        return this;
    }

    /**
     * Gets the value for client behavior for response checksum calculation
     */
    @Override
    public ResponseChecksumValidation responseChecksumValidation() {
        return config.option(SdkClientOption.RESPONSE_CHECKSUM_VALIDATION);
    }

    /**
     * Sets the value for client behavior for request checksum calculation
     */
    @Override
    public JsonServiceClientConfiguration.Builder requestChecksumCalculation(RequestChecksumCalculation requestChecksumCalculation) {
        config.option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION, requestChecksumCalculation);
        return this;
    }

    /**
     * Gets the value for client behavior for request checksum calculation
     */
    @Override
    public RequestChecksumCalculation requestChecksumCalculation() {
        return config.option(SdkClientOption.REQUEST_CHECKSUM_CALCULATION);
    }

    @Override
    public JsonServiceClientConfiguration build() {
        return new JsonServiceClientConfiguration(this);
    }
}
