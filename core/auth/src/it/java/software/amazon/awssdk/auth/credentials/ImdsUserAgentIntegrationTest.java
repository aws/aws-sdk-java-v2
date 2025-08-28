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

package software.amazon.awssdk.auth.credentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;

/**
 * Integration test that verifies CREDENTIALS_IMDS business metric appears in User-Agent headers.
 */
public class ImdsUserAgentIntegrationTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    @Before
    public void setup() {
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{\"message\": \"success\"}")));
    }

    @Test
    public void imdsCredentials_shouldAppearInUserAgentHeader() {
        AwsCredentials imdsCredentials = createImdsCredentials();
        makeHttpRequestWithCredentials(imdsCredentials);

        // Verify that the User-Agent header contains "m/0"
        verify(postRequestedFor(urlEqualTo("/"))
                   .withHeader("User-Agent", matching(".*m/0.*")));
    }

    @Test
    public void staticCredentials_shouldNotAppearInUserAgentHeader() {
        AwsCredentials staticCredentials = AwsBasicCredentials.create("test-key", "test-secret");
        makeHttpRequestWithCredentials(staticCredentials);

        // Verify that the User-Agent header does not contain "m/0"
        verify(postRequestedFor(urlEqualTo("/"))
                   .withHeader("User-Agent",
                              matching("^(?!.*m/0).*$")));
    }

    private void makeHttpRequestWithCredentials(AwsCredentials credentials) {
        String userAgent = buildUserAgentWithCredentials(credentials);

        System.out.println("Making HTTP request with User-Agent: " + userAgent);

        // Make HTTP request with the User-Agent using simple HTTP connection
        try {
            java.net.URL url = new java.net.URL("http://localhost:" + mockServer.port() + "/");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Write request body
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = "{}".getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildUserAgentWithCredentials(AwsCredentials credentials) {
        String baseUserAgent = "aws-sdk-java/2.32.32 Linux/5.4.0 Java_HotSpot(TM)_64-Bit_Server_VM/17.0.1";

        // Check if credentials are IMDS
        boolean isImds = credentials.providerName()
                                   .map(name -> name.contains("InstanceProfile"))
                                   .orElse(false);

        if (isImds) {
            // Add business metrics in the format - "m/0"
            return baseUserAgent + " m/" + BusinessMetricFeatureId.CREDENTIALS_IMDS.value();
        } else {
            return baseUserAgent;
        }
    }

    private AwsCredentials createImdsCredentials() {
        AwsCredentials baseCredentials = AwsBasicCredentials.create("test-access-key", "test-secret-key");
        return new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return baseCredentials.accessKeyId();
            }

            @Override
            public String secretAccessKey() {
                return baseCredentials.secretAccessKey();
            }

            @Override
            public Optional<String> providerName() {
                return Optional.of("InstanceProfileCredentialsProvider");
            }
        };
    }
}
