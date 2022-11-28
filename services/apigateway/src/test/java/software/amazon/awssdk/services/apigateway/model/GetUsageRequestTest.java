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

package software.amazon.awssdk.services.apigateway.model;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.concurrent.CompletionException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayAsyncClient;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;

@WireMockTest
class GetUsageRequestTest {

    private int wireMockPort;

    private ApiGatewayClient client;

    private ApiGatewayAsyncClient asyncClient;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        wireMockPort = wmRuntimeInfo.getHttpPort();

        client = ApiGatewayClient.builder()
                                 .credentialsProvider(StaticCredentialsProvider
                                                          .create(AwsBasicCredentials.create("akid", "skid")))
                                 .region(Region.US_WEST_2)
                                 .endpointOverride(URI.create("http://localhost:" + wireMockPort))
                                 .build();

        asyncClient = ApiGatewayAsyncClient.builder()
                                           .credentialsProvider(StaticCredentialsProvider
                                                                    .create(AwsBasicCredentials.create("akid", "skid")))
                                           .region(Region.US_WEST_2)
                                           .endpointOverride(URI.create("http://localhost:" + wireMockPort))
                                           .build();
    }

    @Test
    void marshall_syncMissingUploadIdButValidationDisabled_ThrowsException() {
        stubAndRespondWith(200, "");

        GetUsageRequest request = getUsageRequestWithDates(null, "20221115");

        assertThatNoException().isThrownBy(() -> client.getUsage(request));
    }

    @Test
    void marshall_syncEmptyStartDate_encodesAsEmptyValue() {
        stubAndRespondWith(200, "");

        GetUsageRequest request = getUsageRequestWithDates("", "20221115");
        client.getUsage(request);

        verify(anyRequestedFor(anyUrl()).withQueryParam("startDate", equalTo("")));
    }

    @Test
    void marshall_syncNonEmptyStartDate_encodesValue() {
        stubAndRespondWith(200, "");

        GetUsageRequest request = getUsageRequestWithDates("20221101", "20221115");
        client.getUsage(request);

        verify(anyRequestedFor(anyUrl()).withQueryParam("startDate", equalTo("20221101")));
    }

    @Test
    void marshall_asyncMissingStartDateButValidationDisabled_ThrowsException() {
        stubAndRespondWith(200, "");

        GetUsageRequest request = getUsageRequestWithDates(null, "20221115");

        assertThatNoException().isThrownBy(() -> asyncClient.getUsage(request).join());
    }

    @Test
    void marshall_asyncEmptyStartDate_encodesAsEmptyValue() {
        stubAndRespondWith(200, "");

        GetUsageRequest request = getUsageRequestWithDates("", "20221115");
        asyncClient.getUsage(request).join();

        verify(anyRequestedFor(anyUrl()).withQueryParam("startDate", equalTo("")));
    }

    @Test
    void marshall_asyncNonEmptyStartDate_encodesValue() {
        stubAndRespondWith(200, "");

        GetUsageRequest request = getUsageRequestWithDates("20221101", "20221115");
        asyncClient.getUsage(request).join();

        verify(anyRequestedFor(anyUrl()).withQueryParam("startDate", equalTo("20221101")));
    }

    private static GetUsageRequest getUsageRequestWithDates(String startDate, String endDate) {
        return GetUsageRequest.builder()
                              .usagePlanId("myUsagePlanId")
                              .startDate(startDate)
                              .endDate(endDate)
                              .build();
    }

    private static void stubAndRespondWith(int status, String body) {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse().withStatus(status).withBody(body)));
    }

}
