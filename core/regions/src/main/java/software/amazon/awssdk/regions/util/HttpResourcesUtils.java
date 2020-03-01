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

package software.amazon.awssdk.regions.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.regions.internal.util.ConnectionUtils;
import software.amazon.awssdk.utils.IoUtils;

@SdkProtectedApi
public final class HttpResourcesUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpResourcesUtils.class);

    private static volatile HttpResourcesUtils instance;

    private final ConnectionUtils connectionUtils;

    private HttpResourcesUtils() {
        this(ConnectionUtils.create());
    }

    HttpResourcesUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    public static HttpResourcesUtils instance() {
        if (instance == null) {
            synchronized (HttpResourcesUtils.class) {
                if (instance == null) {
                    instance = new HttpResourcesUtils();
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
        return readResource(() -> endpoint, "GET");
    }

    /**
     * Connects to the given endpoint to read the resource
     * and returns the text contents.
     *
     * @param endpointProvider The endpoint provider.
     * @return The text payload returned from the container metadata endpoint
     * service for the specified resource path.
     * @throws IOException If any problems were encountered while connecting to the
     * service for the requested resource path.
     * @throws SdkClientException If the requested service is not found.
     */
    public String readResource(ResourcesEndpointProvider endpointProvider) throws IOException {
        return readResource(endpointProvider, "GET");
    }

    /**
     * Connects to the given endpoint to read the resource
     * and returns the text contents.
     *
     * @param endpointProvider The endpoint provider.
     * @param method The HTTP request method to use.
     * @return The text payload returned from the container metadata endpoint
     * service for the specified resource path.
     * @throws IOException If any problems were encountered while connecting to the
     * service for the requested resource path.
     * @throws SdkClientException If the requested service is not found.
     */
    public String readResource(ResourcesEndpointProvider endpointProvider, String method) throws IOException {
        int retriesAttempted = 0;
        InputStream inputStream = null;

        while (true) {
            try {
                HttpURLConnection connection = connectionUtils.connectToEndpoint(endpointProvider.endpoint(),
                                                                                 endpointProvider.headers(),
                                                                                 method);

                int statusCode = connection.getResponseCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    return IoUtils.toUtf8String(inputStream);
                } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // This is to preserve existing behavior of EC2 Instance metadata service.
                    throw SdkClientException.builder()
                                            .message("The requested metadata is not found at " + connection.getURL())
                                            .build();
                } else {
                    if (!endpointProvider.retryPolicy().shouldRetry(retriesAttempted++,
                                                                    ResourcesEndpointRetryParameters.builder()
                                                                                                    .withStatusCode(statusCode)
                                                                                                    .build())) {
                        inputStream = connection.getErrorStream();
                        handleErrorResponse(inputStream, statusCode, connection.getResponseMessage());
                    }
                }
            } catch (IOException ioException) {
                if (!endpointProvider.retryPolicy().shouldRetry(retriesAttempted++,
                                                                ResourcesEndpointRetryParameters.builder()
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
        // Parse the error stream returned from the service.
        if (errorStream != null) {
            String errorResponse = IoUtils.toUtf8String(errorStream);

            try {
                JsonNode node = JacksonUtils.jsonNodeOf(errorResponse);
                JsonNode code = node.get("code");
                JsonNode message = node.get("message");
                if (code != null && message != null) {
                    responseMessage = message.asText();
                }
            } catch (RuntimeException exception) {
                log.debug("Unable to parse error stream", exception);
            }
        }

        SdkServiceException exception = SdkServiceException.builder()
                                                           .message(responseMessage)
                                                           .statusCode(statusCode)
                                                           .build();
        throw exception;
    }
}
