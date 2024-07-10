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
            RuleResult result = endpointRule0(params, new LocalState());
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

    private static RuleResult endpointRule0(QueryEndpointParams params, LocalState locals) {
        return endpointRule1(params, locals);
    }

    private static RuleResult endpointRule1(QueryEndpointParams params, LocalState locals) {
        RulePartition partitionResult = null;
        if ((partitionResult = RulesFunctions.awsPartition(params.regionId())) != null) {
            locals = locals.toBuilder().partitionResult(partitionResult).build();
            RuleResult result = endpointRule2(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule6(params, locals);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule11(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule2(QueryEndpointParams params, LocalState locals) {
        if (params.endpointId() != null) {
            RuleResult result = endpointRule3(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule4(params, locals);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule5(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule3(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() != null && params.useFipsEndpoint()) {
            return RuleResult.error("FIPS endpoints not supported with multi-region endpoints");
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule4(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() == null && params.useDualStackEndpoint() != null && params.useDualStackEndpoint()) {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://" + params.endpointId() + ".query."
                                              + locals.partitionResult().dualStackDnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4aAuthScheme.builder().signingName("query")
                                                                .signingRegionSet(Arrays.asList("*")).build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule5(QueryEndpointParams params, LocalState locals) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + params.endpointId() + ".query." + locals.partitionResult().dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                         .build())).build());
    }

    private static RuleResult endpointRule6(QueryEndpointParams params, LocalState locals) {
        if (RulesFunctions.isValidHostLabel(params.regionId(), false)) {
            RuleResult result = endpointRule7(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule8(params, locals);
            if (result.isResolved()) {
                return result;
            }
            result = endpointRule9(params, locals);
            if (result.isResolved()) {
                return result;
            }
            return endpointRule10(params, locals);
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule7(QueryEndpointParams params, LocalState locals) {
        if (params.useFipsEndpoint() != null && params.useFipsEndpoint() && params.useDualStackEndpoint() == null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://query-fips." + params.regionId() + "." + locals.partitionResult().dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule8(QueryEndpointParams params, LocalState locals) {
        if (params.useDualStackEndpoint() != null && params.useDualStackEndpoint() && params.useFipsEndpoint() == null) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://query." + params.regionId() + "." + locals.partitionResult().dualStackDnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build(), SigV4AuthScheme.builder().signingName("query").signingRegion(params.regionId())
                                                                                                      .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule9(QueryEndpointParams params, LocalState locals) {
        if (params.useDualStackEndpoint() != null && params.useFipsEndpoint() != null && params.useDualStackEndpoint()
            && params.useFipsEndpoint()) {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://query-fips." + params.regionId() + "."
                                                           + locals.partitionResult().dualStackDnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().signingName("query").signingRegionSet(Arrays.asList("*"))
                                                                             .build())).build());
        }
        return RuleResult.carryOn();
    }

    private static RuleResult endpointRule10(QueryEndpointParams params, LocalState locals) {
        return RuleResult.endpoint(Endpoint.builder()
                                           .url(URI.create("https://query." + params.regionId() + "." + locals.partitionResult().dnsSuffix())).build());
    }

    private static RuleResult endpointRule11(QueryEndpointParams params, LocalState locals) {
        return RuleResult.error(params.regionId() + " is not a valid HTTP host-label");
    }

    @Override
    public boolean equals(Object rhs) {
        return rhs != null && getClass().equals(rhs.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private static final class LocalState {
        private final RulePartition partitionResult;

        LocalState() {
            this.partitionResult = null;
        }

        LocalState(LocalStateBuilder builder) {
            this.partitionResult = builder.partitionResult;
        }

        public RulePartition partitionResult() {
            return this.partitionResult;
        }

        public LocalStateBuilder toBuilder() {
            return new LocalStateBuilder(this);
        }
    }

    private static final class LocalStateBuilder {
        private RulePartition partitionResult;

        LocalStateBuilder() {
            this.partitionResult = null;
        }

        LocalStateBuilder(LocalState locals) {
            this.partitionResult = locals.partitionResult;
        }

        public LocalStateBuilder partitionResult(RulePartition value) {
            this.partitionResult = value;
            return this;
        }

        LocalState build() {
            return new LocalState(this);
        }
    }
}
