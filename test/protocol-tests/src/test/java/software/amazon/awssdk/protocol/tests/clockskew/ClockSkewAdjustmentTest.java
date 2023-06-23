/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.protocol.tests.clockskew;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocoljsonrpc.ProtocolJsonRpcAsyncClient;
import software.amazon.awssdk.services.protocoljsonrpc.ProtocolJsonRpcClient;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesResponse;
import software.amazon.awssdk.utils.DateUtils;

public class ClockSkewAdjustmentTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private static final String SCENARIO = "scenario";
    private static final String PATH = "/";
    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";

    private ProtocolJsonRpcClient client;
    private ProtocolJsonRpcAsyncClient asyncClient;

    @Before
    public void setupClient() {
        SdkGlobalTime.setGlobalTimeOffset(0);
        client = createClient(1);
        asyncClient = createAsyncClient(1);
    }

    @Test
    public void clockSkewAdjustsOnClockSkewErrors() {
        assertAdjusts(Instant.now().plus(5, MINUTES), 400, "RequestTimeTooSkewed");
        assertAdjusts(Instant.now().minus(5, MINUTES), 400, "RequestTimeTooSkewed");
        assertAdjusts(Instant.now().plus(1, HOURS), 400, "RequestTimeTooSkewed");
        assertAdjusts(Instant.now().minus(2, HOURS), 400, "RequestTimeTooSkewed");
        assertAdjusts(Instant.now().plus(3, HOURS), 400, "InvalidSignatureException");
        assertAdjusts(Instant.now().minus(4, HOURS), 400, "InvalidSignatureException");
        assertAdjusts(Instant.now().plus(5, HOURS), 403, "");
        assertAdjusts(Instant.now().minus(6, HOURS), 403, "");
    }

    @Test
    public void clockSkewDoesNotAdjustOnNonClockSkewErrors() {
        // Force client clock forward 1 hour
        Instant clientTime = Instant.now().plus(1, HOURS);
        assertAdjusts(clientTime, 400, "RequestTimeTooSkewed");

        // Verify scenarios that should not adjust the client time
        assertNoAdjust(clientTime, clientTime.plus(1, HOURS), 500, "");
        assertNoAdjust(clientTime, clientTime.minus(1, HOURS), 500, "");
        assertNoAdjust(clientTime, clientTime.plus(1, HOURS), 404, "");
        assertNoAdjust(clientTime, clientTime.minus(1, HOURS), 404, "");
        assertNoAdjust(clientTime, clientTime.plus(1, HOURS), 300, "BandwidthLimitExceeded");
        assertNoAdjust(clientTime, clientTime.minus(1, HOURS), 300, "BandwidthLimitExceeded");
        assertNoAdjust(clientTime, clientTime.plus(1, HOURS), 500, "PriorRequestNotComplete");
        assertNoAdjust(clientTime, clientTime.minus(1, HOURS), 500, "PriorRequestNotComplete");
    }

    @Test
    public void clientClockSkewAdjustsWithoutRetries() {
        try (ProtocolJsonRpcClient client = createClient(0)) {
            clientClockSkewAdjustsWithoutRetries(client::allTypes);
        }

        try (ProtocolJsonRpcAsyncClient client = createAsyncClient(0)) {
            clientClockSkewAdjustsWithoutRetries(() -> client.allTypes().join());
        }
    }

    private void clientClockSkewAdjustsWithoutRetries(Runnable call) {
        Instant actualTime = Instant.now();
        Instant skewedTime = actualTime.plus(7, HOURS);

        // Force the client time forward
        stubForResponse(skewedTime, 400, "RequestTimeTooSkewed");
        assertThatThrownBy(call::run).isInstanceOfAny(SdkException.class, CompletionException.class);

        // Verify the next call uses that time
        stubForResponse(actualTime, 200, "");
        call.run();
        assertSigningDateApproximatelyEquals(getRecordedRequests().get(0), skewedTime);
    }

    private void assertNoAdjust(Instant clientTime, Instant serviceTime, int statusCode, String errorCode) {
        assertNoAdjust(clientTime, serviceTime, statusCode, errorCode, () -> client.allTypes());
        assertNoAdjust(clientTime, serviceTime, statusCode, errorCode, () -> asyncClient.allTypes().join());
    }

    private void assertAdjusts(Instant serviceTime, int statusCode, String errorCode) {
        assertAdjusts(serviceTime, statusCode, errorCode, () -> client.allTypes());
        assertAdjusts(serviceTime, statusCode, errorCode, () -> asyncClient.allTypes().join());
    }

    private void assertNoAdjust(Instant clientTime, Instant serviceTime, int statusCode, String errorCode, Runnable methodCall) {
        stubForResponse(serviceTime, statusCode, errorCode);

        assertThatThrownBy(methodCall::run).isInstanceOfAny(SdkException.class, CompletionException.class);

        List<LoggedRequest> requests = getRecordedRequests();
        assertThat(requests.size()).isGreaterThanOrEqualTo(1);

        requests.forEach(r -> assertSigningDateApproximatelyEquals(r, clientTime));
    }

    private void assertAdjusts(Instant serviceTime, int statusCode, String errorCode, Supplier<AllTypesResponse> methodCall) {
        stubForClockSkewFailureThenSuccess(serviceTime, statusCode, errorCode);
        assertThat(methodCall.get().stringMember()).isEqualTo("foo");

        List<LoggedRequest> requests = getRecordedRequests();
        assertThat(requests.size()).isEqualTo(2);

        assertSigningDateApproximatelyEquals(requests.get(1), serviceTime);
    }

    private Instant parseSigningDate(String signatureDate) {
        return Instant.from(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                                             .withZone(ZoneId.of("UTC"))
                                             .parse(signatureDate));
    }

    private void stubForResponse(Instant serviceTime, int statusCode, String errorCode) {
        WireMock.reset();

        stubFor(post(urlEqualTo(PATH))
                        .willReturn(aResponse()
                                            .withStatus(statusCode)
                                            .withHeader("x-amzn-ErrorType", errorCode)
                                            .withHeader("Date", DateUtils.formatRfc822Date(serviceTime))
                                            .withBody("{}")));
    }

    private void stubForClockSkewFailureThenSuccess(Instant serviceTime, int statusCode, String errorCode) {
        WireMock.reset();

        stubFor(post(urlEqualTo(PATH))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willSetStateTo("1")
                        .willReturn(aResponse()
                                            .withStatus(statusCode)
                                            .withHeader("x-amzn-ErrorType", errorCode)
                                            .withHeader("Date", DateUtils.formatRfc822Date(serviceTime))
                                            .withBody("{}")));

        stubFor(post(urlEqualTo(PATH))
                        .inScenario(SCENARIO)
                        .whenScenarioStateIs("1")
                        .willSetStateTo("2")
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withBody(JSON_BODY)));
    }

    private void assertSigningDateApproximatelyEquals(LoggedRequest request, Instant expectedTime) {
        assertThat(parseSigningDate(request.getHeader("X-Amz-Date"))).isBetween(expectedTime.minusSeconds(10), expectedTime.plusSeconds(10));
    }

    private List<LoggedRequest> getRecordedRequests() {
        return findAll(postRequestedFor(urlEqualTo(PATH)));
    }

    private ProtocolJsonRpcClient createClient(int retryCount) {
        return ProtocolJsonRpcClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                    .overrideConfiguration(c -> c.retryStrategy(r -> r.maxAttempts(retryCount + 1)))
                                    .build();
    }

    private ProtocolJsonRpcAsyncClient createAsyncClient(int retryCount) {
        return ProtocolJsonRpcAsyncClient.builder()
                                         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                         .region(Region.US_EAST_1)
                                         .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                         .overrideConfiguration(c -> c.retryStrategy(r -> r.maxAttempts(retryCount + 1)))
                                         .build();
    }
}
