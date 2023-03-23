package software.amazon.awssdk.services.jsonprotocoltests;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
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
        Builder region(Region region);

        /**
         * Configure the client override configuration
         */
        Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);
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
        public JsonProtocolTestsServiceClientConfiguration build() {
            return new JsonProtocolTestsServiceClientConfiguration(this);
        }
    }
}
