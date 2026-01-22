package software.amazon.awssdk.services.jsonprotocoltests.internal;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsServiceClientConfiguration;
import software.amazon.awssdk.services.jsonprotocoltests.auth.scheme.JsonProtocolTestsAuthSchemeProvider;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class JsonProtocolTestsServiceClientConfigurationBuilder implements JsonProtocolTestsServiceClientConfiguration.Builder {
    private final SdkClientConfiguration.Builder config;

    public JsonProtocolTestsServiceClientConfigurationBuilder() {
        this(SdkClientConfiguration.builder());
    }

    public JsonProtocolTestsServiceClientConfigurationBuilder(SdkClientConfiguration.Builder config) {
        this.config = config;
    }

    /**
     * Sets the value for client override configuration
     */
    @Override
    public JsonProtocolTestsServiceClientConfiguration.Builder overrideConfiguration(
        ClientOverrideConfiguration overrideConfiguration) {
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
    public JsonProtocolTestsServiceClientConfiguration.Builder endpointOverride(URI endpointOverride) {
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
    public JsonProtocolTestsServiceClientConfiguration.Builder endpointProvider(EndpointProvider endpointProvider) {
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
    public JsonProtocolTestsServiceClientConfiguration.Builder region(Region region) {
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
    public JsonProtocolTestsServiceClientConfiguration.Builder credentialsProvider(
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
    public JsonProtocolTestsServiceClientConfiguration.Builder putAuthScheme(AuthScheme<?> authScheme) {
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
    public JsonProtocolTestsServiceClientConfiguration.Builder authSchemeProvider(
        JsonProtocolTestsAuthSchemeProvider authSchemeProvider) {
        config.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
        return this;
    }

    /**
     * Gets the value for auth scheme provider
     */
    @Override
    public JsonProtocolTestsAuthSchemeProvider authSchemeProvider() {
        AuthSchemeProvider result = config.option(SdkClientOption.AUTH_SCHEME_PROVIDER);
        if (result == null) {
            return null;
        }
        return Validate.isInstanceOf(JsonProtocolTestsAuthSchemeProvider.class, result, "Expected an instance of "
                                                                                        + JsonProtocolTestsAuthSchemeProvider.class.getSimpleName());
    }

    @Override
    public JsonProtocolTestsServiceClientConfiguration build() {
        return new JsonProtocolTestsServiceClientConfiguration(this);
    }
}
