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

package software.amazon.awssdk.core.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.core.internal.net.ConnectionUtils;
import software.amazon.awssdk.core.retry.internal.CredentialsEndpointRetryParameters;
import software.amazon.awssdk.core.retry.internal.CredentialsEndpointRetryPolicy;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.utils.IoUtils;

@SdkInternalApi
public final class EC2CredentialsUtils {

    private static final Logger log = LoggerFactory.getLogger(EC2CredentialsUtils.class);

    private static volatile EC2CredentialsUtils instance;

    private final ConnectionUtils connectionUtils;

    private EC2CredentialsUtils() {
        this(ConnectionUtils.getInstance());
    }

    EC2CredentialsUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    public static EC2CredentialsUtils getInstance() {
        if (instance == null) {
            synchronized (EC2CredentialsUtils.class) {
                if (instance == null) {
                    instance = new EC2CredentialsUtils();
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
     * @param endpoint
     *            The service endpoint to connect to.
     *
     * @return The text payload returned from the Amazon EC2 endpoint
     *         service for the specified resource path.
     *
     * @throws IOException
     *             If any problems were encountered while connecting to the
     *             service for the requested resource path.
     * @throws SdkClientException
     *             If the requested service is not found.
     */
    public String readResource(URI endpoint) throws IOException {
        return readResource(endpoint, CredentialsEndpointRetryPolicy.NO_RETRY);
    }

    /**
     * Connects to the given endpoint to read the resource
     * and returns the text contents.
     *
     * @param endpoint
     *            The service endpoint to connect to.
     *
     * @param retryPolicy
     *            The custom retry policy that determines whether a
     *            failed request should be retried or not.
     *
     * @return The text payload returned from the Amazon EC2 endpoint
     *         service for the specified resource path.
     *
     * @throws IOException
     *             If any problems were encountered while connecting to the
     *             service for the requested resource path.
     * @throws SdkClientException
     *             If the requested service is not found.
     */
    public String readResource(URI endpoint, CredentialsEndpointRetryPolicy retryPolicy) throws IOException {
        int retriesAttempted = 0;
        InputStream inputStream = null;

        while (true) {
            try {
                HttpURLConnection connection = connectionUtils.connectToEndpoint(endpoint);

                int statusCode = connection.getResponseCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    return IoUtils.toString(inputStream);
                } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // This is to preserve existing behavior of EC2 Instance metadata service.
                    throw new SdkClientException("The requested metadata is not found at " + connection.getURL());
                } else {
                    if (!retryPolicy.shouldRetry(retriesAttempted++,
                                                 CredentialsEndpointRetryParameters.builder()
                                                                                   .withStatusCode(statusCode).build())) {
                        inputStream = connection.getErrorStream();
                        handleErrorResponse(inputStream, statusCode, connection.getResponseMessage());
                    }
                }
            } catch (IOException ioException) {
                if (!retryPolicy.shouldRetry(retriesAttempted++,
                                             CredentialsEndpointRetryParameters.builder().withException(ioException).build())) {
                    throw ioException;
                }
                log.debug("An IOException occurred when connecting to endpoint: {} \n Retrying to connect again", endpoint);

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

        AmazonServiceException ase = new AmazonServiceException(responseMessage);
        ase.setStatusCode(statusCode);
        ase.setErrorCode(errorCode);
        throw ase;
    }
}
