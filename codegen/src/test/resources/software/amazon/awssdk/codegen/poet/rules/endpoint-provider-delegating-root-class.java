package software.amazon.awssdk.services.query.endpoints.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
        Validate.notNull(params.useFipsEndpoint(), "Parameter 'useFIPSEndpoint' must not be null");
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
        if (params.endpoint() != null) {
            return RuleResult.endpoint(Endpoint.builder().endpointUrl(EndpointUrl.fromString(params.endpoint())).build());
        }
        return RuleResult.endpoint(Endpoint.builder()
                .endpointUrl(EndpointUrl.fromComponents("https", "query.amazonaws.com", -1, "")).build());
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
