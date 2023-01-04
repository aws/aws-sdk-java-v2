package software.amazon.awssdk.services.query.endpoints;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.rules.testing.AsyncTestCase;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetClientTest;
import software.amazon.awssdk.core.rules.testing.SyncTestCase;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.query.QueryAsyncClient;
import software.amazon.awssdk.services.query.QueryAsyncClientBuilder;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.QueryClientBuilder;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.ChecksumStructure;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;

@Generated("software.amazon.awssdk:codegen")
public class QueryClientEndpointTests extends BaseRuleSetClientTest {
    @BeforeEach
    public void methodSetup() {
        super.methodSetup();
    }

    @AfterAll
    public static void teardown() {
    }

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
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-1"));
                APostOperationRequest request = APostOperationRequest.builder().build();
                builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://foo-myservice.aws")).build()).build()),
            new SyncTestCase("test case 2", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-1"));
                builder.booleanContextParam(true);
                builder.stringContextParam("this is a test");
                APostOperationRequest request = APostOperationRequest.builder().build();
                builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://foo-myservice.aws")).build()).build()),
            new SyncTestCase("test case 3", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-1"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test").build();
                builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()),
            new SyncTestCase("test case 4", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.region(Region.of("us-east-6"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test").build();
                builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build(),
                             "Does not work"),
            new SyncTestCase("For region us-iso-west-1 with FIPS enabled and DualStack enabled", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                APostOperationRequest request = APostOperationRequest.builder().build();
                builder.build().aPostOperation(request);
            }, Expect.builder().error("Should have been skipped!").build(), "Client builder does the validation"),
            new SyncTestCase("Has complex operation input", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .nestedMember(ChecksumStructure.builder().checksumMode("foo").build()).build();
                builder.build().operationWithContextParam(request);
            }, Expect.builder().error("Missing info").build()));
    }

    private static List<AsyncTestCase> asyncTestCases() {
        return Arrays.asList(
            new AsyncTestCase("test case 1", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-1"));
                APostOperationRequest request = APostOperationRequest.builder().build();
                return builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://foo-myservice.aws")).build()).build()),
            new AsyncTestCase("test case 2", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-1"));
                builder.booleanContextParam(true);
                builder.stringContextParam("this is a test");
                APostOperationRequest request = APostOperationRequest.builder().build();
                return builder.build().aPostOperation(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://foo-myservice.aws")).build()).build()),
            new AsyncTestCase("test case 3", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-1"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test").build();
                return builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()),
            new AsyncTestCase("test case 4", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.region(Region.of("us-east-6"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test").build();
                return builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build(),
                              "Does not work"),
            new AsyncTestCase("For region us-iso-west-1 with FIPS enabled and DualStack enabled", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                APostOperationRequest request = APostOperationRequest.builder().build();
                return builder.build().aPostOperation(request);
            }, Expect.builder().error("Should have been skipped!").build(), "Client builder does the validation"),
            new AsyncTestCase("Has complex operation input", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.CREDENTIALS_PROVIDER);
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .nestedMember(ChecksumStructure.builder().checksumMode("foo").build()).build();
                return builder.build().operationWithContextParam(request);
            }, Expect.builder().error("Missing info").build()));
    }
}
