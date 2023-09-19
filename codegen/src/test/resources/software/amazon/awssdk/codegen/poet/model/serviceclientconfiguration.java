package software.amazon.awssdk.services.jsonprotocoltests;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.SdkInternalAdvancedClientOption;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.jsonprotocoltests.auth.scheme.JsonProtocolTestsAuthSchemeProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * Class to expose the service client settings to the user. Implementation of {@link AwsServiceClientConfiguration}
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class JsonProtocolTestsServiceClientConfiguration extends AwsServiceClientConfiguration {
    private JsonProtocolTestsServiceClientConfiguration(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Builder builder(SdkClientConfiguration.Builder builder) {
        return new BuilderImpl(builder);
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
         * Sets the value for credentials provider
         */
        @Override
        Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider);

        /**
         * Gets the value for credentials provider
         */
        @Override
        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider();

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

    private static final class BuilderImpl implements Builder {
        private final SdkClientConfiguration.Builder builder;

        private ClientOverrideConfiguration overrideConfiguration;

        private BuilderImpl() {
            this.builder = SdkClientConfiguration.builder();
        }

        private BuilderImpl(SdkClientConfiguration.Builder builder) {
            this.builder = builder;
        }

        /**
         * Sets the value for client override configuration
         */
        @Override
        public Builder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        /**
         * Gets the value for client override configuration
         */
        @Override
        public ClientOverrideConfiguration overrideConfiguration() {
            return overrideConfiguration;
        }

        /**
         * Sets the value for AWS region
         */
        @Override
        public Builder region(Region region) {
            builder.option(AwsClientOption.AWS_REGION, region);
            return this;
        }

        /**
         * Gets the value for AWS region
         */
        @Override
        public Region region() {
            return builder.option(AwsClientOption.AWS_REGION);
        }

        /**
         * Sets the value for endpoint override
         */
        @Override
        public Builder endpointOverride(URI endpointOverride) {
            builder.option(SdkInternalAdvancedClientOption.ENDPOINT_OVERRIDE_VALUE, endpointOverride);
            return this;
        }

        /**
         * Gets the value for endpoint override
         */
        @Override
        public URI endpointOverride() {
            return builder.option(SdkInternalAdvancedClientOption.ENDPOINT_OVERRIDE_VALUE);
        }

        /**
         * Sets the value for endpoint provider
         */
        @Override
        public Builder endpointProvider(EndpointProvider endpointProvider) {
            builder.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
            return this;
        }

        /**
         * Gets the value for endpoint provider
         */
        @Override
        public EndpointProvider endpointProvider() {
            return builder.option(SdkClientOption.ENDPOINT_PROVIDER);
        }

        /**
         * Sets the value for credentials provider
         */
        @Override
        public Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            builder.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, credentialsProvider);
            return this;
        }

        /**
         * Gets the value for credentials provider
         */
        @Override
        public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
            return builder.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
        }

        /**
         * Sets the value for auth scheme provider
         */
        @Override
        public Builder authSchemeProvider(JsonProtocolTestsAuthSchemeProvider authSchemeProvider) {
            builder.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
            return this;
        }

        /**
         * Gets the value for auth scheme provider
         */
        @Override
        public JsonProtocolTestsAuthSchemeProvider authSchemeProvider() {
            AuthSchemeProvider result = builder.option(SdkClientOption.AUTH_SCHEME_PROVIDER);
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

        public SdkClientConfiguration buildSdkClientConfiguration() {
            if (overrideConfiguration != null) {
                overrideConfiguration.addOverridesToConfiguration(builder);
            }
            return builder.build();
        }
    }
}
