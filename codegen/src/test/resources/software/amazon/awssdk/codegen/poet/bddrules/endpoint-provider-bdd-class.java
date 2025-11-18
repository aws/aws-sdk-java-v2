package software.amazon.awssdk.services.query.endpoints.internal;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointParams;
import software.amazon.awssdk.services.query.endpoints.QueryEndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultQueryEndpointProvider implements QueryEndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams params) {
        Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
        Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
        try {
            Region region = params.region();
            String regionId = region == null ? null : region.id();
            RuleResult result = endpointRule0(params, regionId);
            if (result.canContinue()) {
                throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
            }
            if (result.isError()) {
                String errorMsg = result.error();
                if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                    errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                }
                throw SdkClientException.create(errorMsg);
            }
            return CompletableFuture.completedFuture(result.endpoint());
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static RuleResult endpointRule0(QueryEndpointParams params, String region) {
        RuleResult result = endpointRule1(params);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule8(params, region);
    }

    private static RuleResult endpointRule1(QueryEndpointParams params) {
        if (params.endpoint() != null) {
            return endpointRule2(params);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule2(QueryEndpointParams params) {
        if (params.useFips()) {
            return RuleResult.error("Invalid Configuration: FIPS and custom endpoint are not supported");
        }
        return endpointRule4(params);
    }

    private static RuleResult endpointRule4(QueryEndpointParams params) {
        return endpointRule5(params);
    }

    private static RuleResult endpointRule5(QueryEndpointParams params) {
        if (params.useDualStack()) {
            return RuleResult.error("Invalid Configuration: Dualstack and custom endpoint are not supported");
        }
        return RuleResult.endpoint(Endpoint.builder().url(URI.create(params.endpoint())).build());
    }

    private static RuleResult endpointRule8(QueryEndpointParams params, String region) {
        return endpointRule9(params, region);
    }

    private static RuleResult endpointRule9(QueryEndpointParams params, String region) {
        RuleResult result = endpointRule10(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule10(QueryEndpointParams params, String region) {
        if (region != null) {
            return endpointRule11(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule11(QueryEndpointParams params, String region) {
        RuleResult result = endpointRule12(params, region);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule31(params, region);
    }

    private static RuleResult endpointRule12(QueryEndpointParams params, String region) {
        if (params.useFips()) {
            return endpointRule13(params, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule13(QueryEndpointParams params, String region) {
        RuleResult result = endpointRule14(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule14(QueryEndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            return endpointRule15(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule15(QueryEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule16(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return endpointRule23(params, partitionResult, region);
    }

    private static RuleResult endpointRule16(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useDualStack()) {
            return endpointRule17(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule17(QueryEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule18(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static RuleResult endpointRule18(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsFIPS()) {
            return endpointRule19(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule19(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsDualStack()) {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://query-fips." + region + "." + partitionResult.dualStackDnsSuffix())).build());
        }
        return RuleResult.error("FIPS and DualStack are enabled, but this partition does not support one or both");
    }

    private static RuleResult endpointRule23(QueryEndpointParams params, RulePartition partitionResult, String region) {
        return endpointRule24(params, partitionResult, region);
    }

    private static RuleResult endpointRule24(QueryEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule25(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("FIPS is enabled but this partition does not support FIPS");
    }

    private static RuleResult endpointRule25(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsFIPS()) {
            return endpointRule26(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule26(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if ("aws-us-gov".equals(partitionResult.name())) {
            return RuleResult.endpoint(Endpoint.builder().url(URI.create("https://query." + region + ".amazonaws.com")).build());
        }
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://query-fips." + region + "." + partitionResult.dnsSuffix())).build());
    }

    private static RuleResult endpointRule31(QueryEndpointParams params, String region) {
        return endpointRule32(params, region);
    }

    private static RuleResult endpointRule32(QueryEndpointParams params, String region) {
        RuleResult result = endpointRule33(params, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.error("Invalid Configuration: Missing Region");
    }

    private static RuleResult endpointRule33(QueryEndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            return endpointRule34(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule34(QueryEndpointParams params, RulePartition partitionResult, String region) {
        RuleResult result = endpointRule35(params, partitionResult, region);
        if (result.isResolved()) {
            return result;
        }
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://query." + region + "." + partitionResult.dnsSuffix())).build());
    }

    private static RuleResult endpointRule35(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if (params.useDualStack()) {
            return endpointRule36(params, partitionResult, region);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule36(QueryEndpointParams params, RulePartition partitionResult, String region) {
        if (partitionResult.supportsDualStack()) {
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://query." + region + "." + partitionResult.dualStackDnsSuffix())).build());
        }
        return RuleResult.error("DualStack is enabled but this partition does not support DualStack");
    }

    @Override
    public boolean equals(Object rhs) {
        return rhs != null && getClass().equals(rhs.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
