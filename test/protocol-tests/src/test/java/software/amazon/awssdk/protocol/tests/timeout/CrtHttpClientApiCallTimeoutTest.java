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

package software.amazon.awssdk.protocol.tests.timeout;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;

@WireMockTest
public class CrtHttpClientApiCallTimeoutTest {

    private ProtocolRestJsonClientBuilder clientBuilder;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wiremock) {
        clientBuilder = ProtocolRestJsonClient.builder()
                                              .region(Region.US_WEST_1)
                                              .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                              .httpClientBuilder(AwsCrtHttpClient.builder())
                                              .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"));
    }

    @Test
    void apiCallAttemptExceeded_shouldThrowApiCallAttemptTimeoutException() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(2000)));
        try (ProtocolRestJsonClient client =
                 clientBuilder.overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMillis(10)))
                                                          .build()) {


            assertThatThrownBy(() -> client.allTypes()).isInstanceOf(ApiCallAttemptTimeoutException.class);
        }
    }

    @Test
    void apiCallExceeded_shouldThrowApiCallAttemptTimeoutException() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(2000)));
        try (ProtocolRestJsonClient client = clientBuilder.overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(10)))
                                                          .build()) {

            assertThatThrownBy(() -> client.allTypes()).isInstanceOf(ApiCallTimeoutException.class);
        }
    }
}
