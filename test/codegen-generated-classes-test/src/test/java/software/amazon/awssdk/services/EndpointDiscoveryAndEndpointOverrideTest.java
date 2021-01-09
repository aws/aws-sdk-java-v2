package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.endpointdiscoveryrequiredtest.EndpointDiscoveryRequiredTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoveryrequiredtest.EndpointDiscoveryRequiredTestClient;
import software.amazon.awssdk.services.endpointdiscoveryrequiredwithcustomizationtest.EndpointDiscoveryRequiredWithCustomizationTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoveryrequiredwithcustomizationtest.EndpointDiscoveryRequiredWithCustomizationTestClient;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestClient;

/**
 * Verify the behavior of endpoint discovery when combined with endpoint override configuration.
 */
@RunWith(Parameterized.class)
public class EndpointDiscoveryAndEndpointOverrideTest {
    private static final String OPTIONAL_SERVICE_ENDPOINT = "https://awsendpointdiscoverytestservice.us-west-2.amazonaws.com";
    private static final String REQUIRED_SERVICE_ENDPOINT = "https://awsendpointdiscoveryrequiredtestservice.us-west-2.amazonaws.com";
    private static final String REQUIRED_CUSTOMIZED_SERVICE_ENDPOINT = "https://awsendpointdiscoveryrequiredwithcustomizationtestservice.us-west-2.amazonaws.com";
    private static final String ENDPOINT_OVERRIDE = "https://endpointoverride";

    private static final List<TestCase<?>> ALL_TEST_CASES = new ArrayList<>();

    private final TestCase<?> testCase;

    static {
        // This first case (case 0/1) is different than other SDKs/the SEP. This should probably actually throw an exception.
        ALL_TEST_CASES.addAll(endpointDiscoveryOptionalCases(true, true, ENDPOINT_OVERRIDE + "/DescribeEndpoints", ENDPOINT_OVERRIDE + "/TestDiscoveryOptional"));
        ALL_TEST_CASES.addAll(endpointDiscoveryOptionalCases(true, false, OPTIONAL_SERVICE_ENDPOINT + "/DescribeEndpoints", OPTIONAL_SERVICE_ENDPOINT + "/TestDiscoveryOptional"));
        ALL_TEST_CASES.addAll(endpointDiscoveryOptionalCases(false, true, ENDPOINT_OVERRIDE + "/TestDiscoveryOptional"));
        ALL_TEST_CASES.addAll(endpointDiscoveryOptionalCases(false, false, OPTIONAL_SERVICE_ENDPOINT + "/TestDiscoveryOptional"));

        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredCases(true, true));
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredCases(true, false, REQUIRED_SERVICE_ENDPOINT + "/DescribeEndpoints"));
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredCases(false, true));
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredCases(false, false));

        // These cases are different from what one would expect. Even though endpoint discovery is required (based on the model),
        // if the customer specifies an endpoint override AND the service is customized, we actually bypass endpoint discovery.
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredAndCustomizedCases(true, true, ENDPOINT_OVERRIDE + "/TestDiscoveryRequired"));
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredAndCustomizedCases(true, false, REQUIRED_CUSTOMIZED_SERVICE_ENDPOINT + "/DescribeEndpoints"));
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredAndCustomizedCases(false, true, ENDPOINT_OVERRIDE + "/TestDiscoveryRequired"));
        ALL_TEST_CASES.addAll(endpointDiscoveryRequiredAndCustomizedCases(false, false));
    }

    public EndpointDiscoveryAndEndpointOverrideTest(TestCase<?> testCase) {
        this.testCase = testCase;
    }

    @Before
    public void reset() {
        EndpointCapturingInterceptor.reset();
    }

    @Parameterized.Parameters(name = "{index} - {0}")
    public static List<TestCase<?>> testCases() {
        return ALL_TEST_CASES;
    }

    @Test(timeout = 5_000)
    public void invokeTestCase() {
        try {
            testCase.callClient();
            Assert.fail();
        } catch (Throwable e) {
            // Unwrap async exceptions so that they can be tested the same as async ones.
            if (e instanceof CompletionException) {
                e = e.getCause();
            }

            if (testCase.expectedPaths.length > 0) {
                // We're using fake endpoints, so we expect even "valid" requests to fail because of unknown host exceptions.
                assertThat(e.getCause()).hasRootCauseInstanceOf(UnknownHostException.class);
            } else {
                // If the requests are not expected to go through, we expect to see illegal state exceptions because the
                // client is configured incorrectly.
                assertThat(e).isInstanceOf(IllegalStateException.class);
            }
        }

        if (testCase.enforcePathOrder) {
            assertThat(EndpointCapturingInterceptor.ENDPOINTS).containsExactly(testCase.expectedPaths);
        } else {
            // Async is involved when order doesn't matter, so wait a little while until the expected number of paths arrive.
            while (EndpointCapturingInterceptor.ENDPOINTS.size() < testCase.expectedPaths.length) {
                Thread.yield();
            }
            assertThat(EndpointCapturingInterceptor.ENDPOINTS).containsExactlyInAnyOrder(testCase.expectedPaths);
        }
    }

    private static List<TestCase<?>> endpointDiscoveryOptionalCases(boolean endpointDiscoveryEnabled,
                                                                    boolean endpointOverridden,
                                                                    String... expectedEndpoints) {
        TestCase<?> syncCase = new TestCase<>(createClient(EndpointDiscoveryTestClient.builder().endpointDiscoveryEnabled(endpointDiscoveryEnabled),
                                                           endpointOverridden),
                                              c -> c.testDiscoveryOptional(r -> {}),
                                              caseName(EndpointDiscoveryTestClient.class, endpointDiscoveryEnabled, endpointOverridden, expectedEndpoints),
                                              false,
                                              expectedEndpoints);

        TestCase<?> asyncCase = new TestCase<>(createClient(EndpointDiscoveryTestAsyncClient.builder().endpointDiscoveryEnabled(endpointDiscoveryEnabled),
                                                            endpointOverridden),
                                               c -> c.testDiscoveryOptional(r -> {}).join(),
                                               caseName(EndpointDiscoveryTestAsyncClient.class, endpointDiscoveryEnabled, endpointOverridden, expectedEndpoints),
                                               false,
                                               expectedEndpoints);

        return Arrays.asList(syncCase, asyncCase);
    }

    private static List<TestCase<?>> endpointDiscoveryRequiredCases(boolean endpointDiscoveryEnabled,
                                                                    boolean endpointOverridden,
                                                                    String... expectedEndpoints) {
        TestCase<?> syncCase = new TestCase<>(createClient(EndpointDiscoveryRequiredTestClient.builder().endpointDiscoveryEnabled(endpointDiscoveryEnabled),
                                                           endpointOverridden),
                                              c -> c.testDiscoveryRequired(r -> {}),
                                              caseName(EndpointDiscoveryRequiredTestClient.class, endpointDiscoveryEnabled, endpointOverridden, expectedEndpoints),
                                              true,
                                              expectedEndpoints);

        TestCase<?> asyncCase = new TestCase<>(createClient(EndpointDiscoveryRequiredTestAsyncClient.builder().endpointDiscoveryEnabled(endpointDiscoveryEnabled),
                                                            endpointOverridden),
                                               c -> c.testDiscoveryRequired(r -> {}).join(),
                                               caseName(EndpointDiscoveryRequiredTestAsyncClient.class, endpointDiscoveryEnabled, endpointOverridden, expectedEndpoints),
                                               true,
                                               expectedEndpoints);

        return Arrays.asList(syncCase, asyncCase);
    }

    private static List<TestCase<?>> endpointDiscoveryRequiredAndCustomizedCases(boolean endpointDiscoveryEnabled,
                                                                                 boolean endpointOverridden,
                                                                                 String... expectedEndpoints) {
        TestCase<?> syncCase = new TestCase<>(createClient(EndpointDiscoveryRequiredWithCustomizationTestClient.builder().endpointDiscoveryEnabled(endpointDiscoveryEnabled),
                                                           endpointOverridden),
                                              c -> c.testDiscoveryRequired(r -> {}),
                                              caseName(EndpointDiscoveryRequiredWithCustomizationTestClient.class, endpointDiscoveryEnabled, endpointOverridden, expectedEndpoints),
                                              true,
                                              expectedEndpoints);

        TestCase<?> asyncCase = new TestCase<>(createClient(EndpointDiscoveryRequiredWithCustomizationTestAsyncClient.builder().endpointDiscoveryEnabled(endpointDiscoveryEnabled),
                                                            endpointOverridden),
                                               c -> c.testDiscoveryRequired(r -> {}).join(),
                                               caseName(EndpointDiscoveryRequiredWithCustomizationTestAsyncClient.class, endpointDiscoveryEnabled, endpointOverridden, expectedEndpoints),
                                               true,
                                               expectedEndpoints);

        return Arrays.asList(syncCase, asyncCase);
    }

    private static <T> T createClient(AwsClientBuilder<?, T> clientBuilder,
                                      boolean endpointOverridden) {
        return clientBuilder.region(Region.US_WEST_2)
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                            .applyMutation(c -> addEndpointOverride(c, endpointOverridden))
                            .overrideConfiguration(c -> c.retryPolicy(p -> p.numRetries(0))
                                                         .addExecutionInterceptor(new EndpointCapturingInterceptor()))
                            .build();
    }

    private static String caseName(Class<?> client,
                                   boolean endpointDiscoveryEnabled,
                                   boolean endpointOverridden,
                                   String... expectedEndpoints) {
        return "(Client=" + client.getSimpleName() +
               ", DiscoveryEnabled=" + endpointDiscoveryEnabled +
               ", EndpointOverridden=" + endpointOverridden +
               ") => (ExpectedEndpoints=" + Arrays.toString(expectedEndpoints) + ")";
    }

    private static void addEndpointOverride(SdkClientBuilder<?, ?> builder, boolean endpointOverridden) {
        if (endpointOverridden) {
            builder.endpointOverride(URI.create(ENDPOINT_OVERRIDE));
        }
    }

    private static class TestCase<T> {
        private final T client;
        private final Consumer<T> methodCall;
        private final String caseName;
        private final boolean enforcePathOrder;
        private final String[] expectedPaths;

        private TestCase(T client, Consumer<T> methodCall, String caseName, boolean enforcePathOrder, String... expectedPaths) {
            this.client = client;
            this.methodCall = methodCall;
            this.caseName = caseName;
            this.enforcePathOrder = enforcePathOrder;
            this.expectedPaths = expectedPaths;
        }

        private void callClient() {
            methodCall.accept(client);
        }

        @Override
        public String toString() {
            return caseName;
        }
    }

    private static class EndpointCapturingInterceptor implements ExecutionInterceptor {
        private static final List<String> ENDPOINTS = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            ENDPOINTS.add(context.httpRequest().getUri().toString());
        }

        private static void reset() {
            ENDPOINTS.clear();
        }
    }
}
