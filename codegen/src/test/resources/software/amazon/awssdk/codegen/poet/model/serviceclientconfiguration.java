package software.amazon.awssdk.services.jsonprotocoltests;

import java.net.URI;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.regions.Region;

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

    /**
     * A builder for creating a {@link JsonProtocolTestsServiceClientConfiguration}
     */
    public interface Builder extends AwsServiceClientConfiguration.Builder {
        @Override
        JsonProtocolTestsServiceClientConfiguration build();

        /**
         * Configure the region
         */
        @Override
        Builder region(Region region);

        /**
         * Configure the endpointOverride
         */
        @Override
        Builder endpointOverride(URI endpointOverride);

        /**
         * Configure the client override configuration
         */
        @Override
        Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);

        /**
         * Configure the endpointProvider
         */
        @Override
        Builder endpointProvider(EndpointProvider endpointProvider);

    }

    private static final class BuilderImpl extends AwsServiceClientConfiguration.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(JsonProtocolTestsServiceClientConfiguration serviceClientConfiguration) {
            super(serviceClientConfiguration);
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
            this.overrideConfiguration = clientOverrideConfiguration;
            return this;
        }

        @Override
        public Builder endpointOverride(URI endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        @Override
        public Builder endpointProvider(EndpointProvider endpointProvider) {
            this.endpointProvider = endpointProvider;
            return this;
        }

        @Override
        public JsonProtocolTestsServiceClientConfiguration build() {
            return new JsonProtocolTestsServiceClientConfiguration(this);
        }
    }
}
