package software.amazon.awssdk.services.samplesvc.endpoints;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.rules.testing.BaseEndpointProviderTest;
import software.amazon.awssdk.core.rules.testing.EndpointProviderTestCase;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;

@Generated("software.amazon.awssdk:codegen")
public class SampleSvcEndpointProviderTests extends BaseEndpointProviderTest {
    private static final SampleSvcEndpointProvider PROVIDER = SampleSvcEndpointProvider.defaultProvider();

    @MethodSource("testCases")
    @ParameterizedTest
    public void resolvesCorrectEndpoint(EndpointProviderTestCase tc) {
        verify(tc);
    }

    private static List<EndpointProviderTestCase> testCases() {
        List<EndpointProviderTestCase> testCases = new ArrayList<>();
        testCases.add(new EndpointProviderTestCase(() -> {
            SampleSvcEndpointParams.Builder builder = SampleSvcEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com/defaultValue1")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            SampleSvcEndpointParams.Builder builder = SampleSvcEndpointParams.builder();
            builder.stringArrayParam(Arrays.asList());
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("no array values set").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            SampleSvcEndpointParams.Builder builder = SampleSvcEndpointParams.builder();
            builder.stringArrayParam(Arrays.asList("staticValue1"));
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com/staticValue1")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            SampleSvcEndpointParams.Builder builder = SampleSvcEndpointParams.builder();
            builder.stringArrayParam(Arrays.asList("key1"));
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com/key1")).build()).build()));
        return testCases;
    }
}
