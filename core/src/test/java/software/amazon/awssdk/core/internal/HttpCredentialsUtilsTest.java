/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.net.ConnectionUtils;
import software.amazon.awssdk.core.retry.internal.CredentialsEndpointRetryParameters;
import software.amazon.awssdk.core.retry.internal.CredentialsEndpointRetryPolicy;
import software.amazon.awssdk.core.util.VersionInfo;
import utils.http.SocketUtils;

@RunWith(MockitoJUnitRunner.class)
public class HttpCredentialsUtilsTest {
    @ClassRule
    public static WireMockRule mockServer = new WireMockRule(wireMockConfig().port(0), false);

    private static final String CREDENTIALS_PATH = "/dummy/credentials/path";
    private static final String SUCCESS_BODY = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                                               + "\"Token\":\"TOKEN_TOKEN_TOKEN\",\"Expiration\":\"3000-05-03T04:55:54Z\"}";
    private static URI endpoint;
    private static Map<String, String> headers = new HashMap<String, String>()
    {
        {
            put("User-Agent", String.format("aws-sdk-java/%s", VersionInfo.SDK_VERSION));
            put("Accept", "*/*");
            put("Connection", "keep-alive");
        }
    };

    private static CustomRetryPolicy customRetryPolicy;

    private static HttpCredentialsUtils httpCredentialsUtils;

    @Mock
    private ConnectionUtils mockConnection;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        endpoint = new URI("http://localhost:" + mockServer.port() + CREDENTIALS_PATH);
        customRetryPolicy = new CustomRetryPolicy();
        httpCredentialsUtils = HttpCredentialsUtils.instance();
    }

    /**
     * When a connection to end host cannot be opened, throws {@link IOException}.
     */
    @Test(expected = IOException.class)
    public void readResourceThrowsIOExceptionWhenNoConnection() throws IOException, URISyntaxException {
        int port = 0;
        try {
            port = SocketUtils.getUnusedPort();
        } catch (IOException ioexception) {
            fail("Unable to find an unused port");
        }

        httpCredentialsUtils.readResource(new URI("http://localhost:" + port));
    }

    /**
     * When server returns with status code 200,
     * the test successfully returns the body from the response.
     */
    @Test
    public void readResouceReturnsResponseBodyFor200Response() throws IOException {
        generateStub(200, SUCCESS_BODY);

        assertEquals(SUCCESS_BODY, httpCredentialsUtils.readResource(endpoint));
    }

    /**
     * When server returns with 404 status code,
     * the test should throw SdkClientException.
     */
    @Test
    public void readResouceReturnsAceFor404ErrorResponse() throws Exception {
        try {
            httpCredentialsUtils.readResource(new URI("http://localhost:" + mockServer.port() + "/dummyPath"));
            fail("Expected SdkClientException");
        } catch (SdkClientException ace) {
            assertTrue(ace.getMessage().contains("The requested metadata is not found at"));
        }
    }

    /**
     * When server returns a status code other than 200 and 404,
     * the test should throw SdkServiceException. The request
     * is not retried.
     */
    @Test
    public void readResouceReturnsServiceExceptionFor5xxResponse() throws IOException {
        generateStub(500, "{\"code\":\"500 Internal Server Error\",\"message\":\"ERROR_MESSAGE\"}");

        try {
            httpCredentialsUtils.readResource(endpoint);
            fail("Expected SdkServiceException");
        } catch (SdkServiceException exception) {
            assertEquals(500, exception.statusCode());
            assertEquals("500 Internal Server Error", exception.errorCode());
            assertEquals("ERROR_MESSAGE", exception.errorMessage());
        }
    }

    /**
     * When server returns a status code other than 200 and 404
     * and error body message is not in Json format,
     * the test throws SdkServiceException.
     */
    @Test
    public void readResouceNonJsonErrorBody() throws IOException {
        generateStub(500, "Non Json error body");

        try {
            httpCredentialsUtils.readResource(endpoint);
            fail("Expected SdkServiceException");
        } catch (SdkServiceException exception) {
            assertEquals(500, exception.statusCode());
            assertNotNull(exception.errorMessage());
        }
    }

    /**
     * When readResource is called with default retry policy and IOException occurs,
     * the request is not retried.
     */
    @Test
    public void readResouceWithDefaultRetryPolicy_DoesNotRetry_ForIoException() throws IOException {
        Mockito.when(mockConnection.connectToEndpoint(endpoint, headers)).thenThrow(new IOException());

        try {
            new HttpCredentialsUtils(mockConnection).readResource(endpoint);
            fail("Expected an IOexception");
        } catch (IOException exception) {
            Mockito.verify(mockConnection, Mockito.times(1)).connectToEndpoint(endpoint, headers);
        }
    }

    /**
     * When readResource is called with custom retry policy and IOException occurs,
     * the request is retried and the number of retries is equal to the value
     * returned by getMaxRetries method of the custom retry policy.
     */
    @Test
    public void readResouceWithCustomRetryPolicy_DoesRetry_ForIoException() throws IOException {
        Mockito.when(mockConnection.connectToEndpoint(endpoint, headers)).thenThrow(new IOException());

        try {
            new HttpCredentialsUtils(mockConnection).readResource(endpointProvider(endpoint, customRetryPolicy));
            fail("Expected an IOexception");
        } catch (IOException exception) {
            Mockito.verify(mockConnection, Mockito.times(CustomRetryPolicy.MAX_RETRIES + 1)).connectToEndpoint(endpoint, headers);
        }
    }

    /**
     * When readResource is called with custom retry policy
     * and the exception is not an IOException,
     * then the request is not retried.
     */
    @Test
    public void readResouceWithCustomRetryPolicy_DoesNotRetry_ForNonIoException() throws IOException {
        generateStub(500, "Non Json error body");
        Mockito.when(mockConnection.connectToEndpoint(endpoint, headers)).thenCallRealMethod();

        try {
            new HttpCredentialsUtils(mockConnection).readResource(endpointProvider(endpoint, customRetryPolicy));
            fail("Expected an SdkServiceException");
        } catch (SdkServiceException exception) {
            Mockito.verify(mockConnection, Mockito.times(1)).connectToEndpoint(endpoint, headers);
        }
    }

    private void generateStub(int statusCode, String message) {
        stubFor(
                get(urlPathEqualTo(CREDENTIALS_PATH))
                        .willReturn(aResponse()
                                            .withStatus(statusCode)
                                            .withHeader("Content-Type", "application/json")
                                            .withHeader("charset", "utf-8")
                                            .withBody(message)));
    }

    /**
     * Retry policy that retries only if a request fails with an IOException.
     */
    private static class CustomRetryPolicy implements CredentialsEndpointRetryPolicy {

        private static final int MAX_RETRIES = 3;

        @Override
        public boolean shouldRetry(int retriesAttempted, CredentialsEndpointRetryParameters retryParams) {

            if (retriesAttempted >= MAX_RETRIES) {
                return false;
            }

            if (retryParams.getException() != null && retryParams.getException() instanceof IOException) {
                return true;
            }

            return false;
        }
    }

    private static CredentialsEndpointProvider endpointProvider(URI endpoint, CredentialsEndpointRetryPolicy retryPolicy) {
        return new CredentialsEndpointProvider() {
            @Override
            public URI endpoint() throws IOException {
                return endpoint;
            }

            @Override
            public CredentialsEndpointRetryPolicy retryPolicy() {
                return retryPolicy;
            }
        };
    }

}
