package software.amazon.awssdk.services.query.rules;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The parameters object used to resolve an endpoint for the % service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class QueryEndpointParameters {
    private final String region;

    private final Boolean useDualStackEndpoint;

    private final Boolean useFIPSEndpoint;

    private final String endpointId;

    private QueryEndpointParameters(BuilderImpl builder) {
        this.region = builder.region;
        this.useDualStackEndpoint = builder.useDualStackEndpoint;
        this.useFIPSEndpoint = builder.useFIPSEndpoint;
        this.endpointId = builder.endpointId;
    }

    public static Builder builder() {
        new BuilderImpl();
    }

    public String region() {
        return region;
    }

    public Boolean useDualStackEndpoint() {
        return useDualStackEndpoint;
    }

    public Boolean useFIPSEndpoint() {
        return useFIPSEndpoint;
    }

    public String endpointId() {
        return endpointId;
    }

    interface Builder {
        Builder region(String region);

        Builder useDualStackEndpoint(Boolean useDualStackEndpoint);

        Builder useFIPSEndpoint(Boolean useFIPSEndpoint);

        Builder endpointId(String endpointId);

        QueryEndpointParameters build();
    }

    private static class BuilderImpl implements Builder {
        private String region;

        private Boolean useDualStackEndpoint;

        private Boolean useFIPSEndpoint;

        private String endpointId;

        @Override
        public void region(String region) {
            this.region = region;
            return this;
        }

        @Override
        public void useDualStackEndpoint(Boolean useDualStackEndpoint) {
            this.useDualStackEndpoint = useDualStackEndpoint;
            return this;
        }

        @Override
        public void useFIPSEndpoint(Boolean useFIPSEndpoint) {
            this.useFIPSEndpoint = useFIPSEndpoint;
            return this;
        }

        @Override
        public void endpointId(String endpointId) {
            this.endpointId = endpointId;
            return this;
        }

        @Override
        public void build() {
            return new QueryEndpointParameters(this);
        }
    }
}
