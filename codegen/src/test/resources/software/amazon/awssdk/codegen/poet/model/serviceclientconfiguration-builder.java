package software.amazon.awssdk.services.jsonprotocoltests.internal;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.internal.SdkClientConfigurationUtil;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.jsonprotocoltests.JsonProtocolTestsServiceClientConfiguration;
import software.amazon.awssdk.services.jsonprotocoltests.auth.scheme.JsonProtocolTestsAuthSchemeProvider;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class JsonProtocolTestsServiceClientConfigurationBuilder {
    public static JsonProtocolTestsServiceClientConfiguration.Builder builder() {
        return new BuilderImpl();
    }

    public static BuilderInternal builder(SdkClientConfiguration.Builder builder) {
        return new BuilderImpl(builder);
    }

    public interface BuilderInternal extends JsonProtocolTestsServiceClientConfiguration.Builder {
        SdkClientConfiguration buildSdkClientConfiguration();
    }

    public static class BuilderImpl implements BuilderInternal {
        private final SdkClientConfiguration.Builder builder;

        private ClientOverrideConfiguration overrideConfiguration;

        private URI endpointOverride;

        private BuilderImpl() {
            this.builder = SdkClientConfiguration.builder();
        }

        private BuilderImpl(SdkClientConfiguration.Builder builder) {
            this.builder = builder;
            this.overrideConfiguration = SdkClientConfigurationUtil.copyConfigurationToOverrides(
                    ClientOverrideConfiguration.builder(), builder).build();
            if (!Boolean.TRUE.equals(builder.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
                this.endpointOverride = builder.option(SdkClientOption.ENDPOINT);
            }
        }

        /**
         * Sets the value for client override configuration
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder overrideConfiguration(
                ClientOverrideConfiguration overrideConfiguration) {
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
         * Sets the value for endpoint override
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        /**
         * Gets the value for endpoint override
         */
        @Override
        public URI endpointOverride() {
            return endpointOverride;
        }

        /**
         * Sets the value for endpoint provider
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder endpointProvider(EndpointProvider endpointProvider) {
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
         * Sets the value for AWS region
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder region(Region region) {
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
         * Sets the value for credentials provider
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder credentialsProvider(
                IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
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
        public JsonProtocolTestsServiceClientConfiguration.Builder authSchemeProvider(
                JsonProtocolTestsAuthSchemeProvider authSchemeProvider) {
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

        @Override
        public SdkClientConfiguration buildSdkClientConfiguration() {
            if (overrideConfiguration != null) {
                SdkClientConfigurationUtil.copyOverridesToConfiguration(overrideConfiguration, builder);
            }
            if (endpointOverride != null) {
                builder.option(SdkClientOption.ENDPOINT, endpointOverride);
                builder.option(SdkClientOption.ENDPOINT_OVERRIDDEN, true);
            }
            return builder.build();
        }
    }
}
