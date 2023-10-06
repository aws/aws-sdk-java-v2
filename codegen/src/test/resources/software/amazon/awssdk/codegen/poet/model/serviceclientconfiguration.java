package software.amazon.awssdk.services.jsonprotocoltests;

import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.jsonprotocoltests.auth.scheme.JsonProtocolTestsAuthSchemeProvider;
import software.amazon.awssdk.services.jsonprotocoltests.internal.JsonProtocolTestsServiceClientConfigurationBuilder;

/**
 * Class to expose the service client settings to the user. Implementation of {@link AwsServiceClientConfiguration}
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class JsonProtocolTestsServiceClientConfiguration extends AwsServiceClientConfiguration {
    private final JsonProtocolTestsAuthSchemeProvider authSchemeProvider;

    public JsonProtocolTestsServiceClientConfiguration(Builder builder) {
        super(builder);
        this.authSchemeProvider = builder.authSchemeProvider();
    }

    /**
     * Gets the value for auth scheme provider
     */
    public JsonProtocolTestsAuthSchemeProvider authSchemeProvider() {
        return authSchemeProvider;
    }

    public static Builder builder() {
        return JsonProtocolTestsServiceClientConfigurationBuilder.builder();
    }

    /**
     * A builder for creating a {@link JsonProtocolTestsServiceClientConfiguration}
     */
    public interface Builder extends AwsServiceClientConfiguration.Builder {
        /**
         * Sets the value for client override configuration
         */
        @Override
        Builder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration);

        /**
         * Gets the value for client override configuration
         */
        @Override
        ClientOverrideConfiguration overrideConfiguration();

        /**
         * Sets the value for endpoint override
         */
        @Override
        Builder endpointOverride(URI endpointOverride);

        /**
         * Gets the value for endpoint override
         */
        @Override
        URI endpointOverride();

        /**
         * Sets the value for endpoint provider
         */
        @Override
        Builder endpointProvider(EndpointProvider endpointProvider);

        /**
         * Gets the value for endpoint provider
         */
        @Override
        EndpointProvider endpointProvider();

        /**
         * Sets the value for AWS region
         */
        @Override
        Builder region(Region region);

        /**
         * Gets the value for AWS region
         */
        @Override
        Region region();

        /**
         * Sets the value for credentials provider
         */
        @Override
        Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider);

        /**
         * Gets the value for credentials provider
         */
        @Override
        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider();

        @Override
        Builder putAuthScheme(AuthScheme<?> authScheme);

        /**
         * Gets the value for auth schemes
         */
        @Override
        Map<String, AuthScheme<?>> authSchemes();

        /**
         * Sets the value for auth scheme provider
         */
        Builder authSchemeProvider(JsonProtocolTestsAuthSchemeProvider authSchemeProvider);

        /**
         * Gets the value for auth scheme provider
         */
        JsonProtocolTestsAuthSchemeProvider authSchemeProvider();

        @Override
        JsonProtocolTestsServiceClientConfiguration build();
    }
}
