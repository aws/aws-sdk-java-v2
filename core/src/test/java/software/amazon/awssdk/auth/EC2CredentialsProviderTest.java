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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.util.DateUtils;
import software.amazon.awssdk.utils.IoUtils;

public class EC2CredentialsProviderTest {
    @ClassRule
    public static WireMockRule mockServer = new WireMockRule(0);
    /** One minute (in milliseconds) */
    private static final long ONE_MINUTE = 1000L * 60;
    /** Environment variable name for the AWS ECS Container credentials path. */
    private static final String CREDENTIALS_PATH = "/dummy/credentials/path";
    private static String successResponse;

    private static String successResponseWithInvalidBody;

    @BeforeClass
    public static void setup() throws IOException {
        successResponse = IoUtils.toString(EC2CredentialsProviderTest.class.getResourceAsStream("/resources/wiremock/successResponse.json"));
        successResponseWithInvalidBody = IoUtils.toString(EC2CredentialsProviderTest.class.getResourceAsStream("/resources/wiremock/successResponseWithInvalidBody.json"));
    }

    /**
     * Test that loadCredentials returns proper credentials when response from client is in proper Json format.
     */
    @Test
    public void testLoadCredentialsParsesJsonResponseProperly() {
        stubForSuccessResponseWithCustomBody(successResponse);

        EC2CredentialsProvider credentialsProvider = testCredentialsProvider();
        AwsSessionCredentials credentials = (AwsSessionCredentials) credentialsProvider.getCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.sessionToken()).isEqualTo("TOKEN_TOKEN_TOKEN");
    }

    /**
     * Test that when credentials are null and response from client does not have access key/secret key,
     * throws RuntimeException.
     */
    @Test
    public void testLoadCredentialsThrowsAceWhenClientResponseDontHaveKeys() {
        // Stub for success response but without keys in the response body
        stubForSuccessResponseWithCustomBody(successResponseWithInvalidBody);

        EC2CredentialsProvider credentialsProvider = testCredentialsProvider();

        assertThatExceptionOfType(AmazonClientException.class).isThrownBy(credentialsProvider::getCredentials)
                                                              .withMessage("Unable to load credentials from service endpoint.");
    }

    /**
     * Tests how the credentials provider behaves when the
     * server is not running.
     */
    @Test
    public void testNoMetadataService() throws Exception {
        stubForErrorResponse();

        EC2CredentialsProvider credentialsProvider = testCredentialsProvider();

        // When there are no credentials, the provider should throw an exception if we can't connect
        assertThatExceptionOfType(AmazonClientException.class).isThrownBy(credentialsProvider::getCredentials);

        // When there are valid credentials (but need to be refreshed) and the endpoint returns 404 status,
        // the provider should throw an exception.
        stubForSuccessResonseWithCustomExpirationDate(new Date(System.currentTimeMillis() + ONE_MINUTE * 4));
        credentialsProvider.getCredentials(); // loads the credentials that will be expired soon

        stubForErrorResponse();  // Behaves as if server is unavailable.
        assertThatExceptionOfType(AmazonClientException.class).isThrownBy(credentialsProvider::getCredentials);
    }

    @Test
    public void basicCachingFunctionalityWorks() {
        EC2CredentialsProvider credentialsProvider = testCredentialsProvider();

        // Successful load
        stubForSuccessResonseWithCustomExpirationDate(Date.from(Instant.now().plus(Duration.ofDays(10))));
        assertThat(credentialsProvider.getCredentials()).isNotNull();

        // Break the server
        stubForErrorResponse();

        // Still successful load
        assertThat(credentialsProvider.getCredentials()).isNotNull();
    }

    private void stubForSuccessResponseWithCustomBody(String body) {
        stubFor(
                get(urlPathEqualTo(CREDENTIALS_PATH))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withHeader("charset", "utf-8")
                                            .withBody(body)));
    }

    private void stubForSuccessResonseWithCustomExpirationDate(Date expiration) {
        stubForSuccessResponseWithCustomBody("{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                                             + "\"Expiration\":\"" + DateUtils.formatIso8601Date(expiration) + "\"}");
    }

    private void stubForErrorResponse() {
        stubFor(
                get(urlPathEqualTo(CREDENTIALS_PATH))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader("Content-Type", "application/json")
                                            .withHeader("charset", "utf-8")
                                            .withBody("{\"code\":\"404 Not Found\",\"message\":\"DetailedErrorMessage\"}")));
    }


    private EC2CredentialsProvider testCredentialsProvider() {
        return new EC2CredentialsProvider(new TestCredentialsEndpointProvider("http://localhost:" + mockServer.port()),
                                          false, "");
    }

    /**
     * Dummy CredentialsPathProvider that overrides the endpoint
     * and connects to the WireMock server.
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
