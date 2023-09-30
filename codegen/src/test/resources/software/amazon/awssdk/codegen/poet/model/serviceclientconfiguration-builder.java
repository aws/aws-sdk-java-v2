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
import software.amazon.awssdk.identity.spi.IdentityProviders;
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
        private final SdkClientConfiguration.Builder internalBuilder;

        private ClientOverrideConfiguration overrideConfiguration;

        private URI endpointOverride;

        private IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;

        private BuilderImpl() {
            this.internalBuilder = SdkClientConfiguration.builder();
        }

        private BuilderImpl(SdkClientConfiguration.Builder internalBuilder) {
            this.internalBuilder = internalBuilder;
            this.overrideConfiguration = SdkClientConfigurationUtil.copyConfigurationToOverrides(
                    ClientOverrideConfiguration.builder(), internalBuilder).build();
            if (Boolean.TRUE.equals(internalBuilder.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
                this.endpointOverride = internalBuilder.option(SdkClientOption.ENDPOINT);
            }
            this.credentialsProvider = internalBuilder.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);
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
            internalBuilder.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
            return this;
        }

        /**
         * Gets the value for endpoint provider
         */
        @Override
        public EndpointProvider endpointProvider() {
            return internalBuilder.option(SdkClientOption.ENDPOINT_PROVIDER);
        }

        /**
         * Sets the value for AWS region
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder region(Region region) {
            internalBuilder.option(AwsClientOption.AWS_REGION, region);
            return this;
        }

        /**
         * Gets the value for AWS region
         */
        @Override
        public Region region() {
            return internalBuilder.option(AwsClientOption.AWS_REGION);
        }

        /**
         * Sets the value for credentials provider
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder credentialsProvider(
                IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Gets the value for credentials provider
         */
        @Override
        public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
            return credentialsProvider;
        }

        /**
         * Sets the value for auth scheme provider
         */
        @Override
        public JsonProtocolTestsServiceClientConfiguration.Builder authSchemeProvider(
                JsonProtocolTestsAuthSchemeProvider authSchemeProvider) {
            internalBuilder.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeProvider);
            return this;
        }

        /**
         * Gets the value for auth scheme provider
         */
        @Override
        public JsonProtocolTestsAuthSchemeProvider authSchemeProvider() {
            AuthSchemeProvider result = internalBuilder.option(SdkClientOption.AUTH_SCHEME_PROVIDER);
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
                SdkClientConfigurationUtil.copyOverridesToConfiguration(overrideConfiguration, internalBuilder);
            }
            if (endpointOverride != null) {
                internalBuilder.option(SdkClientOption.ENDPOINT, endpointOverride);
                internalBuilder.option(SdkClientOption.ENDPOINT_OVERRIDDEN, true);
            }
            if (credentialsProvider != null
                    && !credentialsProvider.equals(internalBuilder.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER))) {
                internalBuilder.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, credentialsProvider);
                IdentityProviders identityProviders = internalBuilder.option(SdkClientOption.IDENTITY_PROVIDERS);
                if (identityProviders == null) {
                    identityProviders = IdentityProviders.builder().putIdentityProvider(credentialsProvider).build();
                } else {
                    identityProviders = identityProviders.toBuilder().putIdentityProvider(credentialsProvider).build();
                }
                internalBuilder.option(SdkClientOption.IDENTITY_PROVIDERS, identityProviders);
            }
            return internalBuilder.build();
        }
    }
}
