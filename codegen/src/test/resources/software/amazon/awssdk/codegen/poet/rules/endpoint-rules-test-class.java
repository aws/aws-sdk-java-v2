package software.amazon.awssdk.services.query.rules;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.rules.testing.AsyncTestCase;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetClientTest;
import software.amazon.awssdk.core.rules.testing.SyncTestCase;
import software.amazon.awssdk.core.rules.testing.model.Endpoint;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.QueryAsyncClient;
import software.amazon.awssdk.services.query.QueryAsyncClientBuilder;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.QueryClientBuilder;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;

@Generated("software.amazon.awssdk:codegen")
public class QueryClientEndpointTests extends BaseRuleSetClientTest {
    @MethodSource("syncTestCases")
    @ParameterizedTest
    public void syncClient_usesCorrectEndpoint(SyncTestCase tc) {
        runAndVerify(tc);
    }

    @MethodSource("asyncTestCases")
    @ParameterizedTest
    public void asyncClient_usesCorrectEndpoint(AsyncTestCase tc) {
        runAndVerify(tc);
    }

    private static List<SyncTestCase> syncTestCases() {
        return Arrays.asList(
            new SyncTestCase("test case 1", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-1"));
                APostOperationRequest request = APostOperationRequest.builder().build();
                builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()),
            new SyncTestCase("test case 2", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-1"));
                builder.booleanContextParam(true);
                builder.stringContextParam("this is a test");
                APostOperationRequest request = APostOperationRequest.builder().build();
                builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()),
            new SyncTestCase("test case 3", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-1"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test").build();
                builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
    }

    private static List<AsyncTestCase> asyncTestCases() {
        return Arrays.asList(
            new AsyncTestCase("test case 1", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-1"));
                APostOperationRequest request = APostOperationRequest.builder().build();
                return builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()),
            new AsyncTestCase("test case 2", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-1"));
                builder.booleanContextParam(true);
                builder.stringContextParam("this is a test");
                APostOperationRequest request = APostOperationRequest.builder().build();
                return builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()),
            new AsyncTestCase("test case 3", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-1"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test").build();
                return builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
    }
}
