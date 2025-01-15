package software.amazon.awssdk.services.database.auth.scheme.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeParams;
import software.amazon.awssdk.services.database.endpoints.DatabaseEndpointProvider;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultDatabaseAuthSchemeParams implements DatabaseAuthSchemeParams, DatabaseEndpointResolverAware {
    private final String operation;

    private final Region region;

    private final RegionSet regionSet;

    private final Boolean useDualStackEndpoint;

    private final Boolean useFIPSEndpoint;

    private final String accountId;

    private final String operationContextParam;

    private final DatabaseEndpointProvider endpointProvider;

    private DefaultDatabaseAuthSchemeParams(Builder builder) {
        this.operation = Validate.paramNotNull(builder.operation, "operation");
        this.region = builder.region;
        this.regionSet = builder.regionSet;
        this.useDualStackEndpoint = builder.useDualStackEndpoint;
        this.useFIPSEndpoint = builder.useFIPSEndpoint;
        this.accountId = builder.accountId;
        this.operationContextParam = builder.operationContextParam;
        this.endpointProvider = builder.endpointProvider;
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
    public Boolean useDualStackEndpoint() {
        return useDualStackEndpoint;
    }

    @Override
    public Boolean useFipsEndpoint() {
        return useFIPSEndpoint;
    }

    @Override
    public String accountId() {
        return accountId;
    }

    @Override
    public String operationContextParam() {
        return operationContextParam;
    }

    @Override
    public DatabaseEndpointProvider endpointProvider() {
        return endpointProvider;
    }

    @Override
    public DatabaseAuthSchemeParams.Builder toBuilder() {
        return new Builder(this);
    }

    private static final class Builder implements DatabaseAuthSchemeParams.Builder, DatabaseEndpointResolverAware.Builder {
        private String operation;

        private Region region;

        private RegionSet regionSet;

        private Boolean useDualStackEndpoint;

        private Boolean useFIPSEndpoint;

        private String accountId;

        private String operationContextParam;

        private DatabaseEndpointProvider endpointProvider;

        Builder() {
        }

        Builder(DefaultDatabaseAuthSchemeParams params) {
            this.operation = params.operation;
            this.region = params.region;
            this.regionSet = params.regionSet;
            this.useDualStackEndpoint = params.useDualStackEndpoint;
            this.useFIPSEndpoint = params.useFIPSEndpoint;
            this.accountId = params.accountId;
            this.operationContextParam = params.operationContextParam;
            this.endpointProvider = params.endpointProvider;
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
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public Builder operationContextParam(String operationContextParam) {
            this.operationContextParam = operationContextParam;
            return this;
        }

        @Override
        public Builder endpointProvider(DatabaseEndpointProvider endpointProvider) {
            this.endpointProvider = endpointProvider;
            return this;
        }

        @Override
        public DatabaseAuthSchemeParams build() {
            return new DefaultDatabaseAuthSchemeParams(this);
        }
    }
}
