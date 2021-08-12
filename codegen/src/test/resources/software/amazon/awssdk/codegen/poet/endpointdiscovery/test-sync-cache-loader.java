package software.amazon.awssdk.services.endpointdiscoverytest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryCacheLoader;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryEndpoint;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.services.endpointdiscoverytest.model.DescribeEndpointsResponse;
import software.amazon.awssdk.services.endpointdiscoverytest.model.Endpoint;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
class EndpointDiscoveryTestEndpointDiscoveryCacheLoader implements EndpointDiscoveryCacheLoader {
    private final EndpointDiscoveryTestClient client;

    private EndpointDiscoveryTestEndpointDiscoveryCacheLoader(EndpointDiscoveryTestClient client) {
        this.client = client;
    }

    public static EndpointDiscoveryTestEndpointDiscoveryCacheLoader create(EndpointDiscoveryTestClient client) {
        return new EndpointDiscoveryTestEndpointDiscoveryCacheLoader(client);
    }

    @Override
    public CompletableFuture<EndpointDiscoveryEndpoint> discoverEndpoint(EndpointDiscoveryRequest endpointDiscoveryRequest) {
        return CompletableFuture.supplyAsync(() -> {
            AwsRequestOverrideConfiguration requestConfig = AwsRequestOverrideConfiguration.from(endpointDiscoveryRequest
                                                                                                     .overrideConfiguration().orElse(null));
            DescribeEndpointsResponse response = client
                .describeEndpoints(software.amazon.awssdk.services.endpointdiscoverytest.model.DescribeEndpointsRequest
                                       .builder().overrideConfiguration(requestConfig).build());
            List<Endpoint> endpoints = response.endpoints();
            Validate.notEmpty(endpoints, "Endpoints returned by service for endpoint discovery must not be empty.");
            Endpoint endpoint = endpoints.get(0);
            return EndpointDiscoveryEndpoint.builder()
                                            .endpoint(toUri(endpoint.address(), endpointDiscoveryRequest.defaultEndpoint()))
                                            .expirationTime(Instant.now().plus(endpoint.cachePeriodInMinutes(), ChronoUnit.MINUTES)).build();
        });
    }
}
