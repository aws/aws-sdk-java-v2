package software.amazon.awssdk.services.query.endpoints.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
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
        Validate.notNull(params.region(), "Parameter 'region' must not be null");
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
        return endpointRule1(params, region);
    }

    private static RuleResult endpointRule1(QueryEndpointParams params, String region) {
        RulePartition partitionResult = RulesFunctions.awsPartition(region);
        if (partitionResult != null) {
            RuleResult result = endpointRule2(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule6(params, region, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error(region + " is not a valid HTTP host-label");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule2(QueryEndpointParams params, RulePartition partitionResult) {
        if (params.endpointId() != null) {
            if (params.useFipsEndpoint() != null && params.useFipsEndpoint()) {
                return RuleResult.error("FIPS endpoints not supported with multi-region endpoints");
            }
            if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + params.endpointId() + ".query." + partitionResult.dualStackDnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + params.endpointId() + ".query." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build())).putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("1", "2")).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule6(QueryEndpointParams params, String region, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(region, false)) {
            if (params.useFipsEndpoint() != null && params.useFipsEndpoint() && params.useDualStackEndpoint() == null) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://query-fips." + region + "." + partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
            if (params.useDualStackEndpoint() != null && params.useDualStackEndpoint() && params.useFipsEndpoint() == null) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://query." + region + "." + partitionResult.dualStackDnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(),
                                                                 SigV4AuthScheme.builder().signingName("query").signingRegion(region).build())).build());
            }
            if (params.useDualStackEndpoint() != null && params.useFipsEndpoint() != null && params.useDualStackEndpoint()
                && params.useFipsEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://query-fips." + region + "." + partitionResult.dualStackDnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
            return RuleResult.endpoint(Endpoint.builder()
                                               .url(URI.create("https://query." + region + "." + partitionResult.dnsSuffix())).build());
        }
        return RuleResult.carryOn();
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
