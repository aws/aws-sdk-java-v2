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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.net.ConnectionUtils;
import software.amazon.awssdk.core.retry.internal.CredentialsEndpointRetryParameters;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.utils.IoUtils;

@SdkInternalApi
public final class HttpCredentialsUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpCredentialsUtils.class);

    private static volatile HttpCredentialsUtils instance;

    private final ConnectionUtils connectionUtils;

    private HttpCredentialsUtils() {
        this(ConnectionUtils.create());
    }

    HttpCredentialsUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    public static HttpCredentialsUtils instance() {
        if (instance == null) {
            synchronized (HttpCredentialsUtils.class) {
                if (instance == null) {
                    instance = new HttpCredentialsUtils();
                }
            }
        }
        return instance;
    }

    /**
     * Connects to the given endpoint to read the resource
     * and returns the text contents.
     *
     * If the connection fails, the request will not be retried.
     *
     * @param endpoint The service endpoint to connect to.
     * @return The text payload returned from the container metadata endpoint
     * service for the specified resource path.
     * @throws IOException If any problems were encountered while connecting to the
     * service for the requested resource path.
     * @throws SdkClientException If the requested service is not found.
     */
    public String readResource(URI endpoint) throws IOException {
        return readResource(() -> endpoint);
    }

    /**
     * Connects to the given endpoint to read the resource
     * and returns the text contents.
     *
     * @param endpoint The service endpoint to connect to.
     * @param retryPolicy The custom retry policy that determines whether a
     * failed request should be retried or not.
     * @return The text payload returned from the container metadata endpoint
     * service for the specified resource path.
     * @throws IOException If any problems were encountered while connecting to the
     * service for the requested resource path.
     * @throws SdkClientException If the requested service is not found.
     */
    public String readResource(CredentialsEndpointProvider endpointProvider) throws IOException {
        int retriesAttempted = 0;
        InputStream inputStream = null;

        while (true) {
            try {
                HttpURLConnection connection = connectionUtils.connectToEndpoint(endpointProvider.endpoint(),
                                                                                 endpointProvider.headers());

                int statusCode = connection.getResponseCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    return IoUtils.toString(inputStream);
                } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // This is to preserve existing behavior of EC2 Instance metadata service.
                    throw new SdkClientException("The requested metadata is not found at " + connection.getURL());
                } else {
                    if (!endpointProvider.retryPolicy().shouldRetry(retriesAttempted++,
                                                                    CredentialsEndpointRetryParameters.builder()
                                                                                                      .withStatusCode(statusCode)
                                                                                                      .build())) {
                        inputStream = connection.getErrorStream();
                        handleErrorResponse(inputStream, statusCode, connection.getResponseMessage());
                    }
                }
            } catch (IOException ioException) {
                if (!endpointProvider.retryPolicy().shouldRetry(retriesAttempted++,
                                                                CredentialsEndpointRetryParameters.builder()
                                                                                                  .withException(ioException)
                                                                                                  .build())) {
                    throw ioException;
                }
                log.debug("An IOException occurred when connecting to endpoint: {} \n Retrying to connect again",
                          endpointProvider.endpoint());

            } finally {
                IoUtils.closeQuietly(inputStream, log);
            }
        }

    }

    private void handleErrorResponse(InputStream errorStream, int statusCode, String responseMessage) throws IOException {
        String errorCode = null;

        // Parse the error stream returned from the service.
        if (errorStream != null) {
            String errorResponse = IoUtils.toString(errorStream);

            try {
                JsonNode node = JacksonUtils.jsonNodeOf(errorResponse);
                JsonNode code = node.get("code");
                JsonNode message = node.get("message");
                if (code != null && message != null) {
                    errorCode = code.asText();
                    responseMessage = message.asText();
                }
            } catch (RuntimeException exception) {
                log.debug("Unable to parse error stream", exception);
            }
        }

        SdkServiceException exception = new SdkServiceException(responseMessage);
        exception.statusCode(statusCode);
        exception.errorCode(errorCode);
        throw exception;
    }
}
