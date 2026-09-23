package software.amazon.awssdk.services.query.endpoints.internal;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
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
            RuleResult result = endpointRule0(params);
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

    private static RuleResult endpointRule0(QueryEndpointParams params) {
        return endpointRule1(params);
    }

    private static RuleResult endpointRule1(QueryEndpointParams params) {
        RulePartition partitionResult = RulesFunctions.awsPartition(params.regionId());
        if (partitionResult != null) {
            RuleResult result = endpointRule2(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule6(params, partitionResult);
            if (result.isResolved()) {
                return result;
            }
            return RuleResult.error(params.regionId() + " is not a valid HTTP host-label");
            if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint()
                && params.arnList() != null) {
                String firstArn = RulesFunctions.listAccess(params.arnList(), 0);
                if (firstArn != null) {
                    RuleArn parsedArn = RulesFunctions.awsParseArn(firstArn);
                    if (parsedArn != null) {
                        String arnResourceId = RulesFunctions.listAccess(parsedArn.resourceId(), 0);
                        if (arnResourceId != null) {
                            return RuleResult.endpoint(Endpoint
                                                           .builder()
                                                           .endpointUrl(
                                                               EndpointUrl.fromComponents("https", arnResourceId + "." + params.endpointId()
                                                                                                   + ".query." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                                           .putAttribute(
                                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                             .signingRegionSet(Arrays.asList("*")).build())).build());
                        }
                    }
                }
            }
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
                                               .endpointUrl(
                                                   EndpointUrl.fromComponents("https",
                                                                              params.endpointId() + ".query." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https", params.endpointId() + ".query." + partitionResult.dnsSuffix(),
                                                                          -1, ""))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule6(QueryEndpointParams params, RulePartition partitionResult) {
        if (RulesFunctions.isValidHostLabel(params.regionId(), false)) {
            if (params.useFipsEndpoint() != null && params.useFipsEndpoint() && params.useDualStackEndpoint() == null) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .endpointUrl(
                                                   EndpointUrl.fromComponents("https",
                                                                              "query-fips." + params.regionId() + "." + partitionResult.dnsSuffix(), -1, ""))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
            if (params.useDualStackEndpoint() != null && params.useDualStackEndpoint() && params.useFipsEndpoint() == null) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .endpointUrl(
                                                   EndpointUrl.fromComponents("https",
                                                                              "query." + params.regionId() + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(),
                                                                 SigV4AuthScheme.builder().signingName("query").signingRegion(params.regionId()).build()))
                                               .build());
            }
            if (params.useDualStackEndpoint() != null && params.useFipsEndpoint() != null && params.useDualStackEndpoint()
                && params.useFipsEndpoint()) {
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .endpointUrl(
                                                   EndpointUrl.fromComponents("https",
                                                                              "query-fips." + params.regionId() + "." + partitionResult.dualStackDnsSuffix(), -1, ""))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            }
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .endpointUrl(
                                               EndpointUrl.fromComponents("https", "query." + params.regionId() + "." + partitionResult.dnsSuffix(),
                                                                          -1, "")).build());
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
