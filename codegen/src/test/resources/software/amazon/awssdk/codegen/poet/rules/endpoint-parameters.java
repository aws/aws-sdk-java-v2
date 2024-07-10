package software.amazon.awssdk.services.query.endpoints;

import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The parameters object used to resolve an endpoint for the Query service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class QueryEndpointParams implements ToCopyableBuilder<QueryEndpointParams.Builder, QueryEndpointParams> {
    private final Region region;

    private final Boolean useDualStackEndpoint;

    private final Boolean useFIPSEndpoint;

    private final String accountId;

    private final String accountIdEndpointMode;

    private final List<String> listOfStrings;

    private final List<String> defaultListOfStrings;

    private final String endpointId;

    private final Boolean defaultTrueParam;

    private final String defaultStringParam;

    private final String deprecatedParam;

    private final Boolean booleanContextParam;

    private final String stringContextParam;

    private final String operationContextParam;

    private final List<String> customEndpointArray;

    private QueryEndpointParams(BuilderImpl builder) {
        this.region = builder.region;
        this.useDualStackEndpoint = builder.useDualStackEndpoint;
        this.useFIPSEndpoint = builder.useFIPSEndpoint;
        this.accountId = builder.accountId;
        this.accountIdEndpointMode = builder.accountIdEndpointMode;
        this.listOfStrings = builder.listOfStrings;
        this.defaultListOfStrings = builder.defaultListOfStrings;
        this.endpointId = builder.endpointId;
        this.defaultTrueParam = builder.defaultTrueParam;
        this.defaultStringParam = builder.defaultStringParam;
        this.deprecatedParam = builder.deprecatedParam;
        this.booleanContextParam = builder.booleanContextParam;
        this.stringContextParam = builder.stringContextParam;
        this.operationContextParam = builder.operationContextParam;
        this.customEndpointArray = builder.customEndpointArray;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public Region region() {
        return region;
    }

    public String regionId() {
        return region == null ? null : region.id();
    }

    public Boolean useDualStackEndpoint() {
        return useDualStackEndpoint;
    }

    public Boolean useFipsEndpoint() {
        return useFIPSEndpoint;
    }

    public String accountId() {
        return accountId;
    }

    public String accountIdEndpointMode() {
        return accountIdEndpointMode;
    }

    public List<String> listOfStrings() {
        return listOfStrings;
    }

    public List<String> defaultListOfStrings() {
        return defaultListOfStrings;
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

    @Deprecated
    public String deprecatedParam() {
        return deprecatedParam;
    }

    public Boolean booleanContextParam() {
        return booleanContextParam;
    }

    public String stringContextParam() {
        return stringContextParam;
    }

    public String operationContextParam() {
        return operationContextParam;
    }

    public List<String> customEndpointArray() {
        return customEndpointArray;
    }

    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends CopyableBuilder<Builder, QueryEndpointParams> {
        Builder region(Region region);

        Builder useDualStackEndpoint(Boolean useDualStackEndpoint);

        Builder useFipsEndpoint(Boolean useFIPSEndpoint);

        Builder accountId(String accountId);

        Builder accountIdEndpointMode(String accountIdEndpointMode);

        Builder listOfStrings(List<String> listOfStrings);

        Builder defaultListOfStrings(List<String> defaultListOfStrings);

        Builder endpointId(String endpointId);

        Builder defaultTrueParam(Boolean defaultTrueParam);

        Builder defaultStringParam(String defaultStringParam);

        @Deprecated
        Builder deprecatedParam(String deprecatedParam);

        Builder booleanContextParam(Boolean booleanContextParam);

        Builder stringContextParam(String stringContextParam);

        Builder operationContextParam(String operationContextParam);

        Builder customEndpointArray(List<String> customEndpointArray);

        QueryEndpointParams build();
    }

    private static class BuilderImpl implements Builder {
        private Region region;

        private Boolean useDualStackEndpoint;

        private Boolean useFIPSEndpoint;

        private String accountId;

        private String accountIdEndpointMode;

        private List<String> listOfStrings;

        private List<String> defaultListOfStrings = Arrays.asList("item1", "item2", "item3");

        private String endpointId;

        private Boolean defaultTrueParam = true;

        private String defaultStringParam = "hello endpoints";

        private String deprecatedParam;

        private Boolean booleanContextParam;

        private String stringContextParam;

        private String operationContextParam;

        private List<String> customEndpointArray;

        private BuilderImpl() {
        }

        private BuilderImpl(QueryEndpointParams builder) {
            this.region = builder.region;
            this.useDualStackEndpoint = builder.useDualStackEndpoint;
            this.useFIPSEndpoint = builder.useFIPSEndpoint;
            this.accountId = builder.accountId;
            this.accountIdEndpointMode = builder.accountIdEndpointMode;
            this.listOfStrings = builder.listOfStrings;
            this.defaultListOfStrings = builder.defaultListOfStrings;
            this.endpointId = builder.endpointId;
            this.defaultTrueParam = builder.defaultTrueParam;
            this.defaultStringParam = builder.defaultStringParam;
            this.deprecatedParam = builder.deprecatedParam;
            this.booleanContextParam = builder.booleanContextParam;
            this.stringContextParam = builder.stringContextParam;
            this.operationContextParam = builder.operationContextParam;
            this.customEndpointArray = builder.customEndpointArray;
        }

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
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public Builder accountIdEndpointMode(String accountIdEndpointMode) {
            this.accountIdEndpointMode = accountIdEndpointMode;
            return this;
        }

        @Override
        public Builder listOfStrings(List<String> listOfStrings) {
            this.listOfStrings = listOfStrings;
            return this;
        }

        @Override
        public Builder defaultListOfStrings(List<String> defaultListOfStrings) {
            this.defaultListOfStrings = defaultListOfStrings;
            if (this.defaultListOfStrings == null) {
                this.defaultListOfStrings = Arrays.asList("item1", "item2", "item3");
            }
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
            if (this.defaultTrueParam == null) {
                this.defaultTrueParam = true;
            }
            return this;
        }

        @Override
        public Builder defaultStringParam(String defaultStringParam) {
            this.defaultStringParam = defaultStringParam;
            if (this.defaultStringParam == null) {
                this.defaultStringParam = "hello endpoints";
            }
            return this;
        }

        @Deprecated
        @Override
        public Builder deprecatedParam(String deprecatedParam) {
            this.deprecatedParam = deprecatedParam;
            return this;
        }

        @Override
        public Builder booleanContextParam(Boolean booleanContextParam) {
            this.booleanContextParam = booleanContextParam;
            return this;
        }

        @Override
        public Builder stringContextParam(String stringContextParam) {
            this.stringContextParam = stringContextParam;
            return this;
        }

        @Override
        public Builder operationContextParam(String operationContextParam) {
            this.operationContextParam = operationContextParam;
            return this;
        }

        @Override
        public Builder customEndpointArray(List<String> customEndpointArray) {
            this.customEndpointArray = customEndpointArray;
            return this;
        }

        @Override
        public QueryEndpointParams build() {
            return new QueryEndpointParams(this);
        }
    }
}
