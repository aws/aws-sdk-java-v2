package software.amazon.awssdk.services.query.endpoints;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.services.query.endpoints.internal.DefaultQueryEndpointProvider;

/**
 * An endpoint provider for Query. The endpoint provider takes a set of parameters using {@link QueryEndpointParams},
 * and resolves an {@link Endpoint} base on the given parameters.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryEndpointProvider extends EndpointProvider {
    /**
     * Compute the endpoint based on the given set of parameters.
     */
    CompletableFuture<Endpoint> resolveEndpoint(QueryEndpointParams endpointParams);

    /**
     * Compute the endpoint based on the given set of parameters.
     */
    default CompletableFuture<Endpoint> resolveEndpoint(Consumer<QueryEndpointParams.Builder> endpointParamsConsumer) {
        QueryEndpointParams.Builder paramsBuilder = QueryEndpointParams.builder();
        endpointParamsConsumer.accept(paramsBuilder);
        return resolveEndpoint(paramsBuilder.build());
    }

    static QueryEndpointProvider defaultProvider() {
        return new DefaultQueryEndpointProvider();
    }
}
