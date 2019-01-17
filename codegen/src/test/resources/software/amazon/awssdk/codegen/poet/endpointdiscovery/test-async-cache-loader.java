package software.amazon.awssdk.services.endpointdiscoverytest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryCacheLoader;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryEndpoint;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.services.endpointdiscoverytest.model.Endpoint;

@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
class EndpointDiscoveryTestAsyncEndpointDiscoveryCacheLoader implements EndpointDiscoveryCacheLoader {
    private final EndpointDiscoveryTestAsyncClient client;

    EndpointDiscoveryTestAsyncEndpointDiscoveryCacheLoader(EndpointDiscoveryTestAsyncClient client) {
        this.client = client;
    }

    public static EndpointDiscoveryTestAsyncEndpointDiscoveryCacheLoader create(EndpointDiscoveryTestAsyncClient client) {
        return new EndpointDiscoveryTestAsyncEndpointDiscoveryCacheLoader(client);
    }

    @Override
    public CompletableFuture<EndpointDiscoveryEndpoint> discoverEndpoint(EndpointDiscoveryRequest endpointDiscoveryRequest) {
        return client.describeEndpoints(
                software.amazon.awssdk.services.endpointdiscoverytest.model.DescribeEndpointsRequest.builder().build())
                .thenApply(
                        r -> {
                            Endpoint endpoint = r.endpoints().get(0);
                            return EndpointDiscoveryEndpoint.builder()
                                    .endpoint(toUri(endpoint.address(), endpointDiscoveryRequest.defaultEndpoint()))
                                    .expirationTime(Instant.now().plus(endpoint.cachePeriodInMinutes(), ChronoUnit.MINUTES))
                                    .build();
                        });
    }
}
