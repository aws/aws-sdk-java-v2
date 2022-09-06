package software.amazon.awssdk.services.query.rules;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.Region;

/**
 * The parameters object used to resolve an endpoint for the Query service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class QueryEndpointParams {
    private final Region region;

    private final Boolean useDualStackEndpoint;

    private final Boolean useFIPSEndpoint;

    private final String endpointId;

    private final Boolean defaultTrueParam;

    private final String defaultStringParam;

    private final String deprecatedParam;

    private QueryEndpointParams(BuilderImpl builder) {
        this.region = builder.region;
        this.useDualStackEndpoint = builder.useDualStackEndpoint;
        this.useFIPSEndpoint = builder.useFIPSEndpoint;
        this.endpointId = builder.endpointId;
        this.defaultTrueParam = builder.defaultTrueParam;
        this.defaultStringParam = builder.defaultStringParam;
        this.deprecatedParam = builder.deprecatedParam;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public Region region() {
        return region;
    }

    public Boolean useDualStackEndpoint() {
        return useDualStackEndpoint;
    }

    public Boolean useFipsEndpoint() {
        return useFIPSEndpoint;
    }

    public String endpointId() {
        return endpointId;
    }

    public Boolean defaultTrueParam() {
        return defaultTrueParam;
    }

    public String defaultStringParam() {
        return defaultStringParam;
    }

    public String deprecatedParam() {
        return deprecatedParam;
    }

    public interface Builder {
        Builder region(Region region);

        Builder useDualStackEndpoint(Boolean useDualStackEndpoint);

        Builder useFipsEndpoint(Boolean useFIPSEndpoint);

        Builder endpointId(String endpointId);

        Builder defaultTrueParam(Boolean defaultTrueParam);

        Builder defaultStringParam(String defaultStringParam);

        Builder deprecatedParam(String deprecatedParam);

        QueryEndpointParams build();
    }

    private static class BuilderImpl implements Builder {
        private Region region;

        private Boolean useDualStackEndpoint;

        private Boolean useFIPSEndpoint;

        private String endpointId;

        private Boolean defaultTrueParam;

        private String defaultStringParam;

        private String deprecatedParam;

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public Builder useDualStackEndpoint(Boolean useDualStackEndpoint) {
            this.useDualStackEndpoint = useDualStackEndpoint;
            return this;
        }

        @Override
        public Builder useFipsEndpoint(Boolean useFIPSEndpoint) {
            this.useFIPSEndpoint = useFIPSEndpoint;
            return this;
        }

        @Override
        public Builder endpointId(String endpointId) {
            this.endpointId = endpointId;
            return this;
        }

        @Override
        public Builder defaultTrueParam(Boolean defaultTrueParam) {
            this.defaultTrueParam = defaultTrueParam;
            return this;
        }

        @Override
        public Builder defaultStringParam(String defaultStringParam) {
            this.defaultStringParam = defaultStringParam;
            return this;
        }

        @Override
        public Builder deprecatedParam(String deprecatedParam) {
            this.deprecatedParam = deprecatedParam;
            return this;
        }

        @Override
        public QueryEndpointParams build() {
            return new QueryEndpointParams(this);
        }
    }
}
