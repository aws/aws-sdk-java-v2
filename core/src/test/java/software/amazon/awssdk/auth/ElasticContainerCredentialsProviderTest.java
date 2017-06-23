/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.internal.CredentialsEndpointProvider;

/**
 * Tests for the {@link ElasticContainerCredentialsProviderTest}.
 */
public class ElasticContainerCredentialsProviderTest {
    @ClassRule
    public static WireMockRule mockServer = new WireMockRule(0);

    /** Environment variable name for the AWS ECS Container credentials path. */
    private static final String CREDENTIALS_PATH = "/dummy/credentials/path";
    private static final String ACCESS_KEY_ID = "ACCESS_KEY_ID";
    private static final String SECRET_ACCESS_KEY = "SECRET_ACCESS_KEY";
    private static final String TOKEN = "TOKEN_TOKEN_TOKEN";
    private ElasticContainerCredentialsProvider credentialsProvider;

    @Before
    public void setup() {
        TestCredentialsEndpointProvider endpointProvider =
                new TestCredentialsEndpointProvider("http://localhost:" + mockServer.port());
        credentialsProvider = ElasticContainerCredentialsProvider.builder()
                                                                 .credentialsEndpointProvider(endpointProvider)
                                                                 .build();
    }

    /**
     * Tests that when "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" is not set, throws exception.
     */
    @Test(expected = SdkClientException.class)
    public void testEnvVariableNotSet() {
        new ElasticContainerCredentialsProvider().getCredentials();
    }

    /**
     * Tests that the getCredentials returns a value when it receives a valid 200 response from endpoint.
     */
    @Test
    public void testGetCredentialsReturnsValidResponseFromEcsEndpoint() {
        try {
            System.setProperty(AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_PATH.property(), "");

            stubForSuccessResponse();

            AwsSessionCredentials credentials = (AwsSessionCredentials) credentialsProvider.getCredentials();

            assertThat(credentials).isNotNull();
            assertThat(credentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
            assertThat(credentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
            assertThat(credentials.sessionToken()).isEqualTo(TOKEN);
        } finally {
            System.clearProperty(AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_PATH.property());
        }
    }

    private void stubForSuccessResponse() {
        stubFor(
            get(urlPathEqualTo(CREDENTIALS_PATH))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withHeader("charset", "utf-8")
                                .withBody("{\"AccessKeyId\":\"ACCESS_KEY_ID\"," +
                                          "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\"," +
                                          "\"Token\":\"TOKEN_TOKEN_TOKEN\"," +
                                          "\"Expiration\":\"3000-05-03T04:55:54Z\"}")));
    }

    /**
     * Dummy CredentialsPathProvider that overrides the endpoint and connects to the WireMock server.
     */
    private static class TestCredentialsEndpointProvider extends CredentialsEndpointProvider {
        private final String host;

        public TestCredentialsEndpointProvider(String host) {
            this.host = host;
        }

        @Override
        public URI getCredentialsEndpoint() throws URISyntaxException {
            return new URI(host + CREDENTIALS_PATH);
        }
    }
}
