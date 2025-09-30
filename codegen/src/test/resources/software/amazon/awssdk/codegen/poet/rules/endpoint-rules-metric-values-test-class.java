package software.amazon.awssdk.services.query.endpoints;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.core.rules.testing.BaseEndpointProviderTest;
import software.amazon.awssdk.core.rules.testing.EndpointProviderTestCase;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;

@Generated("software.amazon.awssdk:codegen")
public class QueryEndpointProviderTests extends BaseEndpointProviderTest {
    private static final QueryEndpointProvider PROVIDER = QueryEndpointProvider.defaultProvider();

    @MethodSource("testCases")
    @ParameterizedTest
    public void resolvesCorrectEndpoint(EndpointProviderTestCase tc) {
        verify(tc);
    }

    private static List<EndpointProviderTestCase> testCases() {
        List<EndpointProviderTestCase> testCases = new ArrayList<>();
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect
                                                       .builder()
                                                       .endpoint(
                                                           Endpoint.builder().url(URI.create("https://myservice.aws"))
                                                                   .putAttribute(AwsEndpointAttribute.METRIC_VALUES, Arrays.asList("1", "2")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.booleanContextParam(true);
            builder.stringContextParam("this is a test");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.region(Region.of("us-east-6"));
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.accountId("012345678901");
            builder.accountIdEndpointMode("required");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.accountId("012345678901");
            builder.accountIdEndpointMode("required");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            builder.accountId("012345678901");
            builder.accountIdEndpointMode("required");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Should have been skipped!").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Missing info").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Missing info").build()));
        return testCases;
    }
}
