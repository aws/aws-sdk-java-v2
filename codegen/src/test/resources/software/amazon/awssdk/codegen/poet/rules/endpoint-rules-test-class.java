package software.amazon.awssdk.services.query.rules;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.rules.testing.AsyncTestCase;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetTest;
import software.amazon.awssdk.core.rules.testing.SyncTestCase;
import software.amazon.awssdk.core.rules.testing.model.Endpoint;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.services.query.QueryAsyncClient;
import software.amazon.awssdk.services.query.QueryAsyncClientBuilder;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.QueryClientBuilder;
import software.amazon.awssdk.services.query.model.APostOperationRequest;

@Generated("software.amazon.awssdk:codegen")
public class QueryEndpointTests extends BaseRuleSetTest {
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
        return Arrays
            .asList(new SyncTestCase("For region ap-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://query-fips.ap-south-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.ap-south-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-south-1.api.aws")).build()).build()),
                    new SyncTestCase("For region ap-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-south-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region eu-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-south-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.eu-south-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region eu-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-south-1.api.aws")).build()).build()),
                    new SyncTestCase("For region eu-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-south-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region us-gov-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-gov-east-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-gov-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-gov-east-1.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region us-gov-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-gov-east-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-gov-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query.us-gov-east-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region ca-central-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.ca-central-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ca-central-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ca-central-1.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region ca-central-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ca-central-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ca-central-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ca-central-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region eu-central-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-central-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-central-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.eu-central-1.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region eu-central-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-central-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-central-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-central-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region us-west-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-west-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-west-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-west-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region us-west-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-1.api.aws")).build()).build()),
                    new SyncTestCase("For region us-west-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region us-west-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-west-2.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-west-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-west-2.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region us-west-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-2.api.aws")).build()).build()),
                    new SyncTestCase("For region us-west-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-2.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region af-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.af-south-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region af-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.af-south-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region af-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.af-south-1.api.aws")).build()).build()),
                    new SyncTestCase("For region af-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.af-south-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region eu-north-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-north-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-north-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.eu-north-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region eu-north-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-north-1.api.aws")).build()).build()),
                    new SyncTestCase("For region eu-north-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-north-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region eu-west-3 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-west-3.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-west-3 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.eu-west-3.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region eu-west-3 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-3.api.aws")).build()).build()),
                    new SyncTestCase("For region eu-west-3 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-3.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region eu-west-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-west-2.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-west-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.eu-west-2.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region eu-west-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-2.api.aws")).build()).build()),
                    new SyncTestCase("For region eu-west-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-2.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region eu-west-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-west-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region eu-west-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.eu-west-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region eu-west-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-1.api.aws")).build()).build()),
                    new SyncTestCase("For region eu-west-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region ap-northeast-3 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-3.api.aws"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-northeast-3 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-3.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region ap-northeast-3 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-northeast-3.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-northeast-3 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.ap-northeast-3.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region ap-northeast-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-2.api.aws"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-northeast-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-2.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region ap-northeast-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-northeast-2.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-northeast-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.ap-northeast-2.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region ap-northeast-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-1.api.aws"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-northeast-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-1.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region ap-northeast-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-northeast-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-northeast-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.ap-northeast-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region me-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.me-south-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region me-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.me-south-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region me-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.me-south-1.api.aws")).build()).build()),
                    new SyncTestCase("For region me-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.me-south-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region sa-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.sa-east-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region sa-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.sa-east-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region sa-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.sa-east-1.api.aws")).build()).build()),
                    new SyncTestCase("For region sa-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.sa-east-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region ap-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.ap-east-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ap-east-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region ap-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-east-1.api.aws")).build()).build()),
                    new SyncTestCase("For region ap-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-east-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region cn-north-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder()
                                                     .url(URI.create("https://query-fips.cn-north-1.api.amazonwebservices.com.cn"))
                                                     .build()).build()),
                    new SyncTestCase("For region cn-north-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.cn-north-1.amazonaws.com.cn"))
                                                     .build()).build()),
                    new SyncTestCase("For region cn-north-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder()
                                                       .url(URI.create("https://query.cn-north-1.api.amazonwebservices.com.cn"))
                                                       .build()).build()),
                    new SyncTestCase("For region cn-north-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query.cn-north-1.amazonaws.com.cn"))
                                                       .build()).build()),
                    new SyncTestCase("For region us-gov-west-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-gov-west-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-gov-west-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-gov-west-1.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region us-gov-west-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-gov-west-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-gov-west-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query.us-gov-west-1.amazonaws.com"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-southeast-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-1.api.aws"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-southeast-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-1.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region ap-southeast-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-southeast-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-southeast-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.ap-southeast-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region ap-southeast-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-2.api.aws"))
                                                       .build()).build()),
                    new SyncTestCase("For region ap-southeast-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-2.amazonaws.com"))
                                                     .build()).build()),
                    new SyncTestCase("For region ap-southeast-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-southeast-2.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region ap-southeast-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.ap-southeast-2.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region us-iso-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .error("FIPS and DualStack are enabled, but this partition does not support one or both").build()),
                    new SyncTestCase("For region us-iso-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    },
                                     Expect.builder()
                                           .endpoint(
                                               Endpoint.builder().url(URI.create("https://query-fips.us-iso-east-1.c2s.ic.gov"))
                                                       .build()).build()),
                    new SyncTestCase("For region us-iso-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder().error("DualStack is enabled but this partition does not support DualStack").build()),
                    new SyncTestCase("For region us-iso-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-iso-east-1.c2s.ic.gov")).build())
                             .build()),
                    new SyncTestCase("For region us-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-east-1.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-east-1.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region us-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-1.api.aws")).build()).build()),
                    new SyncTestCase("For region us-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-1.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase("For region us-east-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-east-2.api.aws")).build())
                             .build()),
                    new SyncTestCase("For region us-east-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-east-2.amazonaws.com")).build())
                                         .build()),
                    new SyncTestCase("For region us-east-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-2.api.aws")).build()).build()),
                    new SyncTestCase("For region us-east-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-2.amazonaws.com")).build())
                             .build()),
                    new SyncTestCase(
                        "For region cn-northwest-1 with FIPS enabled and DualStack enabled",
                        () -> {
                            QueryClientBuilder builder = QueryClient.builder();
                            builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                            builder.httpClient(getSyncHttpClient());
                            APostOperationRequest request = APostOperationRequest.builder().build();
                            builder.build().aPostOperation(request);
                        },
                        Expect.builder()
                              .endpoint(
                                  Endpoint.builder()
                                          .url(URI.create("https://query-fips.cn-northwest-1.api.amazonwebservices.com.cn"))
                                          .build()).build()),
                    new SyncTestCase("For region cn-northwest-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.cn-northwest-1.amazonaws.com.cn"))
                                                     .build()).build()),
                    new SyncTestCase("For region cn-northwest-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder()
                                                     .url(URI.create("https://query.cn-northwest-1.api.amazonwebservices.com.cn"))
                                                     .build()).build()),
                    new SyncTestCase("For region cn-northwest-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.cn-northwest-1.amazonaws.com.cn"))
                                                     .build()).build()),
                    new SyncTestCase("For region us-isob-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .error("FIPS and DualStack are enabled, but this partition does not support one or both").build()),
                    new SyncTestCase("For region us-isob-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query-fips.us-isob-east-1.sc2s.sgov.gov"))
                                                     .build()).build()),
                    new SyncTestCase("For region us-isob-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder().error("DualStack is enabled but this partition does not support DualStack").build()),
                    new SyncTestCase("For region us-isob-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect
                                         .builder()
                                         .endpoint(
                                             Endpoint.builder().url(URI.create("https://query.us-isob-east-1.sc2s.sgov.gov")).build())
                                         .build()), new SyncTestCase("For custom endpoint with fips disabled and dualstack disabled",
                                                                     () -> {
                                                                         QueryClientBuilder builder = QueryClient.builder();
                                                                         builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                                                                         builder.httpClient(getSyncHttpClient());
                                                                         APostOperationRequest request = APostOperationRequest.builder().build();
                                                                         builder.build().aPostOperation(request);
                                                                     }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com")).build())
                                                                              .build()), new SyncTestCase(
                    "For custom endpoint with fips enabled and dualstack disabled", () -> {
                    QueryClientBuilder builder = QueryClient.builder();
                    builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                    builder.httpClient(getSyncHttpClient());
                    APostOperationRequest request = APostOperationRequest.builder().build();
                    builder.build().aPostOperation(request);
                }, Expect.builder().error("Invalid Configuration: FIPS and custom endpoint are not supported")
                         .build()),
                    new SyncTestCase("For custom endpoint with fips disabled and dualstack enabled", () -> {
                        QueryClientBuilder builder = QueryClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getSyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        builder.build().aPostOperation(request);
                    }, Expect.builder().error("Invalid Configuration: Dualstack and custom endpoint are not supported")
                             .build()));
    }

    private static List<AsyncTestCase> asyncTestCases() {
        return Arrays
            .asList(new AsyncTestCase("For region ap-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://query-fips.ap-south-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.ap-south-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-south-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region ap-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-south-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region eu-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-south-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.eu-south-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region eu-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-south-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region eu-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-south-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region us-gov-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-gov-east-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-gov-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-gov-east-1.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region us-gov-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-gov-east-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-gov-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query.us-gov-east-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ca-central-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.ca-central-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ca-central-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ca-central-1.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region ca-central-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ca-central-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ca-central-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ca-central-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region eu-central-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-central-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-central-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.eu-central-1.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region eu-central-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-central-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-central-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-central-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region us-west-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-west-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-west-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-west-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region us-west-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region us-west-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region us-west-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-west-2.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-west-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-west-2.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region us-west-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-2.api.aws")).build()).build()),
                    new AsyncTestCase("For region us-west-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-west-2.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region af-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.af-south-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region af-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.af-south-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region af-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.af-south-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region af-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.af-south-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region eu-north-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-north-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-north-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.eu-north-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region eu-north-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-north-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region eu-north-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-north-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region eu-west-3 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-west-3.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-west-3 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.eu-west-3.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region eu-west-3 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-3.api.aws")).build()).build()),
                    new AsyncTestCase("For region eu-west-3 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-3.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region eu-west-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-west-2.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-west-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.eu-west-2.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region eu-west-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-2.api.aws")).build()).build()),
                    new AsyncTestCase("For region eu-west-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-2.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region eu-west-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.eu-west-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region eu-west-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.eu-west-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region eu-west-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region eu-west-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.eu-west-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region ap-northeast-3 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-3.api.aws"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-northeast-3 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-3.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region ap-northeast-3 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-northeast-3.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-northeast-3 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.ap-northeast-3.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region ap-northeast-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-2.api.aws"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-northeast-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-2.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region ap-northeast-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-northeast-2.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-northeast-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.ap-northeast-2.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region ap-northeast-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-1.api.aws"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-northeast-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ap-northeast-1.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region ap-northeast-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-northeast-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-northeast-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.ap-northeast-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region me-south-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.me-south-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region me-south-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.me-south-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region me-south-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.me-south-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region me-south-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.me-south-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region sa-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.sa-east-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region sa-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.sa-east-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region sa-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.sa-east-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region sa-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.sa-east-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region ap-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.ap-east-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ap-east-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region ap-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-east-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region ap-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-east-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region cn-north-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder()
                                                      .url(URI.create("https://query-fips.cn-north-1.api.amazonwebservices.com.cn"))
                                                      .build()).build()),
                    new AsyncTestCase("For region cn-north-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.cn-north-1.amazonaws.com.cn"))
                                                      .build()).build()),
                    new AsyncTestCase("For region cn-north-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder()
                                                        .url(URI.create("https://query.cn-north-1.api.amazonwebservices.com.cn"))
                                                        .build()).build()),
                    new AsyncTestCase("For region cn-north-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query.cn-north-1.amazonaws.com.cn"))
                                                        .build()).build()),
                    new AsyncTestCase("For region us-gov-west-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-gov-west-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-gov-west-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-gov-west-1.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region us-gov-west-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-gov-west-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-gov-west-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query.us-gov-west-1.amazonaws.com"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-southeast-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-1.api.aws"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-southeast-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-1.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region ap-southeast-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-southeast-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-southeast-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.ap-southeast-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region ap-southeast-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-2.api.aws"))
                                                        .build()).build()),
                    new AsyncTestCase("For region ap-southeast-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.ap-southeast-2.amazonaws.com"))
                                                      .build()).build()),
                    new AsyncTestCase("For region ap-southeast-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.ap-southeast-2.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region ap-southeast-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.ap-southeast-2.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region us-iso-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .error("FIPS and DualStack are enabled, but this partition does not support one or both").build()),
                    new AsyncTestCase("For region us-iso-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    },
                                      Expect.builder()
                                            .endpoint(
                                                Endpoint.builder().url(URI.create("https://query-fips.us-iso-east-1.c2s.ic.gov"))
                                                        .build()).build()),
                    new AsyncTestCase("For region us-iso-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder().error("DualStack is enabled but this partition does not support DualStack").build()),
                    new AsyncTestCase("For region us-iso-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-iso-east-1.c2s.ic.gov")).build())
                             .build()),
                    new AsyncTestCase("For region us-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-east-1.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-east-1.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region us-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-1.api.aws")).build()).build()),
                    new AsyncTestCase("For region us-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-1.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase("For region us-east-2 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query-fips.us-east-2.api.aws")).build())
                             .build()),
                    new AsyncTestCase("For region us-east-2 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-east-2.amazonaws.com")).build())
                                          .build()),
                    new AsyncTestCase("For region us-east-2 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-2.api.aws")).build()).build()),
                    new AsyncTestCase("For region us-east-2 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .endpoint(Endpoint.builder().url(URI.create("https://query.us-east-2.amazonaws.com")).build())
                             .build()),
                    new AsyncTestCase(
                        "For region cn-northwest-1 with FIPS enabled and DualStack enabled",
                        () -> {
                            QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                            builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                            builder.httpClient(getAsyncHttpClient());
                            APostOperationRequest request = APostOperationRequest.builder().build();
                            return builder.build().aPostOperation(request);
                        },
                        Expect.builder()
                              .endpoint(
                                  Endpoint.builder()
                                          .url(URI.create("https://query-fips.cn-northwest-1.api.amazonwebservices.com.cn"))
                                          .build()).build()),
                    new AsyncTestCase("For region cn-northwest-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.cn-northwest-1.amazonaws.com.cn"))
                                                      .build()).build()),
                    new AsyncTestCase("For region cn-northwest-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder()
                                                      .url(URI.create("https://query.cn-northwest-1.api.amazonwebservices.com.cn"))
                                                      .build()).build()),
                    new AsyncTestCase("For region cn-northwest-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.cn-northwest-1.amazonaws.com.cn"))
                                                      .build()).build()),
                    new AsyncTestCase("For region us-isob-east-1 with FIPS enabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder()
                             .error("FIPS and DualStack are enabled, but this partition does not support one or both").build()),
                    new AsyncTestCase("For region us-isob-east-1 with FIPS enabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query-fips.us-isob-east-1.sc2s.sgov.gov"))
                                                      .build()).build()),
                    new AsyncTestCase("For region us-isob-east-1 with FIPS disabled and DualStack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder().error("DualStack is enabled but this partition does not support DualStack").build()),
                    new AsyncTestCase("For region us-isob-east-1 with FIPS disabled and DualStack disabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect
                                          .builder()
                                          .endpoint(
                                              Endpoint.builder().url(URI.create("https://query.us-isob-east-1.sc2s.sgov.gov")).build())
                                          .build()), new AsyncTestCase("For custom endpoint with fips disabled and dualstack disabled",
                                                                       () -> {
                                                                           QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                                                                           builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                                                                           builder.httpClient(getAsyncHttpClient());
                                                                           APostOperationRequest request = APostOperationRequest.builder().build();
                                                                           return builder.build().aPostOperation(request);
                                                                       }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com")).build())
                                                                                .build()), new AsyncTestCase(
                    "For custom endpoint with fips enabled and dualstack disabled", () -> {
                    QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                    builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                    builder.httpClient(getAsyncHttpClient());
                    APostOperationRequest request = APostOperationRequest.builder().build();
                    return builder.build().aPostOperation(request);
                }, Expect.builder().error("Invalid Configuration: FIPS and custom endpoint are not supported")
                         .build()),
                    new AsyncTestCase("For custom endpoint with fips disabled and dualstack enabled", () -> {
                        QueryAsyncClientBuilder builder = QueryAsyncClient.builder();
                        builder.credentialsProvider(BaseRuleSetTest.CREDENTIALS_PROVIDER);
                        builder.httpClient(getAsyncHttpClient());
                        APostOperationRequest request = APostOperationRequest.builder().build();
                        return builder.build().aPostOperation(request);
                    }, Expect.builder().error("Invalid Configuration: Dualstack and custom endpoint are not supported")
                             .build()));
    }
}
