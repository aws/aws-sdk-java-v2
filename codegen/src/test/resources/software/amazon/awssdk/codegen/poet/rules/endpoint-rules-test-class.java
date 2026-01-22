package software.amazon.awssdk.services.query.endpoints;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
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
import software.amazon.awssdk.services.query.model.AttributeValue;
import software.amazon.awssdk.services.query.model.ChecksumStructure;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamRequest;
import software.amazon.awssdk.utils.ImmutableMap;

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
            new SyncTestCase("test case 5", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.credentialsProviderWithAccountId("012345678901"));
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.accountIdEndpointMode(AccountIdEndpointMode.fromValue("required"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test with AccountId and AccountIdEndpointMode").build();
                builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build())
                     .build()),
            new SyncTestCase("test case 6", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.credentialsProviderWithAccountId("012345678901"));
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.accountIdEndpointMode(AccountIdEndpointMode.fromValue("required"));
                OperationWithMapOperationContextParamRequest request = OperationWithMapOperationContextParamRequest
                    .builder()
                    .operationWithMapOperationContextParam(
                        ImmutableMap.of("key", AttributeValue.builder().s("value").build())).build();
                builder.build().operationWithMapOperationContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build())
                     .build()),
            new SyncTestCase("test case 7", () -> {
                QueryClientBuilder builder = QueryClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.credentialsProviderWithAccountId("012345678901"));
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getSyncHttpClient());
                builder.accountIdEndpointMode(AccountIdEndpointMode.fromValue("required"));
                OperationWithMapOperationContextParamRequest request = OperationWithMapOperationContextParamRequest
                    .builder()
                    .operationWithMapOperationContextParam(
                        ImmutableMap.of("key", AttributeValue.builder().s("value").build(), "key2", AttributeValue
                            .builder().s("value2").build())).build();
                builder.build().operationWithMapOperationContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build())
                     .build()),
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
            new AsyncTestCase("test case 5", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.credentialsProviderWithAccountId("012345678901"));
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.accountIdEndpointMode(AccountIdEndpointMode.fromValue("required"));
                OperationWithContextParamRequest request = OperationWithContextParamRequest.builder()
                                                                                           .stringMember("this is a test with AccountId and AccountIdEndpointMode").build();
                return builder.build().operationWithContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build())
                     .build()),
            new AsyncTestCase("test case 6", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.credentialsProviderWithAccountId("012345678901"));
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.accountIdEndpointMode(AccountIdEndpointMode.fromValue("required"));
                OperationWithMapOperationContextParamRequest request = OperationWithMapOperationContextParamRequest
                    .builder()
                    .operationWithMapOperationContextParam(
                        ImmutableMap.of("key", AttributeValue.builder().s("value").build())).build();
                return builder.build().operationWithMapOperationContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build())
                     .build()),
            new AsyncTestCase("test case 7", () -> {
                QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                builder.credentialsProvider(BaseRuleSetClientTest.credentialsProviderWithAccountId("012345678901"));
                builder.tokenProvider(BaseRuleSetClientTest.TOKEN_PROVIDER);
                builder.httpClient(getAsyncHttpClient());
                builder.accountIdEndpointMode(AccountIdEndpointMode.fromValue("required"));
                OperationWithMapOperationContextParamRequest request = OperationWithMapOperationContextParamRequest
                    .builder()
                    .operationWithMapOperationContextParam(
                        ImmutableMap.of("key", AttributeValue.builder().s("value").build(), "key2", AttributeValue
                            .builder().s("value2").build())).build();
                return builder.build().operationWithMapOperationContextParam(request);
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build())
                     .build()),
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
