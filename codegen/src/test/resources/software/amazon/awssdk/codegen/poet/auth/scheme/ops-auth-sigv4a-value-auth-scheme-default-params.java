package software.amazon.awssdk.services.database.auth.scheme.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeParams;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultDatabaseAuthSchemeParams implements DatabaseAuthSchemeParams {
    private final String operation;

    private final Region region;

    private final RegionSet regionSet;

    private DefaultDatabaseAuthSchemeParams(Builder builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.region = builder.region;
        this.regionSet = builder.regionSet;
    }

    public static DatabaseAuthSchemeParams.Builder builder() {
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
    public RegionSet regionSet() {
        return regionSet;
    }

    @Override
    public DatabaseAuthSchemeParams.Builder toBuilder() {
        return new Builder(this);
    }

    private static final class Builder implements DatabaseAuthSchemeParams.Builder {
        private String operation;

        private Region region;

        private RegionSet regionSet;

        Builder() {
        }

        Builder(DefaultDatabaseAuthSchemeParams params) {
            this.operation = params.operation;
            this.region = params.region;
            this.regionSet = params.regionSet;
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
        public Builder regionSet(RegionSet regionSet) {
            this.regionSet = regionSet;
            return this;
        }

        @Override
        public DatabaseAuthSchemeParams build() {
            return new DefaultDatabaseAuthSchemeParams(this);
        }
    }
}
