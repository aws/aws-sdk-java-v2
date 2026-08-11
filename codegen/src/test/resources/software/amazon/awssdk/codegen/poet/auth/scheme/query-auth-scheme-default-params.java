package software.amazon.awssdk.services.query.auth.scheme.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.auth.scheme.QueryAuthSchemeParams;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryAuthSchemeParams implements QueryAuthSchemeParams {
    private final String operation;

    private final Region region;

    private DefaultQueryAuthSchemeParams(Builder builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.region = builder.region;
    }

    public static QueryAuthSchemeParams.Builder builder() {
        return new Builder();
    }

    @Override
    public String operation() {
        return operation;
    }

    @Override
    public Region region() {
        return region;
    }

    @Override
    public String regionId() {
        return region == null ? null : region.id();
    }

    @Override
    public QueryAuthSchemeParams.Builder toBuilder() {
        return new Builder(this);
    }

    private static final class Builder implements QueryAuthSchemeParams.Builder {
        private String operation;

        private Region region;

        Builder() {
        }

        Builder(DefaultQueryAuthSchemeParams params) {
            this.operation = params.operation;
            this.region = params.region;
        }

        @Override
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public QueryAuthSchemeParams build() {
            return new DefaultQueryAuthSchemeParams(this);
        }
    }
}
