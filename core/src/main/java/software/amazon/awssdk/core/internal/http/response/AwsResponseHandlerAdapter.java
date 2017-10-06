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

package software.amazon.awssdk.core.internal.http.response;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.AmazonWebServiceResponse;
import software.amazon.awssdk.core.SdkStandardLoggers;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

/**
 * Adapts an {@code HttpResponseHandler<AmazonWebServiceResponse<T>>} to an {@code HttpResponseHandler<T>} (unwrapped result)
 * with proper handling and logging of response metadata.
 *
 * @param <T> Unmarshalled result type
 */
@SdkInternalApi
public class AwsResponseHandlerAdapter<T> implements HttpResponseHandler<T> {
    private final HttpResponseHandler<AmazonWebServiceResponse<T>> delegate;

    /**
     * @param delegate          Response handler to delegate to and unwrap
     */
    public AwsResponseHandlerAdapter(HttpResponseHandler<AmazonWebServiceResponse<T>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
        final AmazonWebServiceResponse<T> awsResponse = delegate.handle(response, executionAttributes);

        if (awsResponse == null) {
            throw new RuntimeException("Unable to unmarshall response metadata. Response Code: "
                                       + response.getStatusCode() + ", Response Text: " +
                                       response.getStatusText());
        }

        final String awsRequestId = awsResponse.getRequestId();

        SdkStandardLoggers.REQUEST_LOGGER.debug(() -> "Received successful response: " + response.getStatusCode() +
                                                      ", AWS Request ID: " + awsRequestId);

        if (!logHeaderRequestId(response)) {
            // Logs the AWS request ID extracted from the payload if
            // it is not available from the response header.
            logResponseRequestId(awsRequestId);
        }

        return awsResponse.getResult();
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return delegate.needsConnectionLeftOpen();
    }

    /**
     * Used to log the "x-amzn-RequestId" header at DEBUG level, if any, from the response. This
     * method assumes the apache httpClientSettings request/response has just been successfully
     * executed. The request id is logged using the "software.amazon.awssdk.requestId" logger if it was
     * enabled at DEBUG level; otherwise, it is logged at DEBUG level using the
     * "software.amazon.awssdk.request" logger.
     *
     * @return true if the AWS request id is available from the httpClientSettings header; false
     *     otherwise.
     */
    private boolean logHeaderRequestId(final HttpResponse response) {
        final String reqIdHeader = response.getHeaders()
                                           .get(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER);
        final boolean isHeaderReqIdAvail = reqIdHeader != null;

        logRequestId(() -> HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER + ": "
                           + (isHeaderReqIdAvail ? reqIdHeader : "not available"));

        return isHeaderReqIdAvail;
    }

    /**
     * Used to log the request id (extracted from the response) at DEBUG level. This method is
     * called only if there is no request id present in the httpClientSettings response header. The
     * request id is logged using the "software.amazon.awssdk.requestId" logger if it was enabled at DEBUG
     * level; otherwise, it is logged using at DEBUG level using the "software.amazon.awssdk.request"
     * logger.
     */
    private void logResponseRequestId(final String awsRequestId) {
        logRequestId(() -> "AWS Request ID: " + (awsRequestId == null ? "not available" : awsRequestId));
    }

    /**
     * Logs the provided message using the "software.amazon.awssdk.requestId" logger if it was enabled at DEBUG
     * level; otherwise, it is logged using at DEBUG level using the "software.amazon.awssdk.request" logger.
     */
    private void logRequestId(Supplier<String> message) {
        if (SdkStandardLoggers.REQUEST_ID_LOGGER.isLoggingLevelEnabled("debug")) {
            SdkStandardLoggers.REQUEST_ID_LOGGER.debug(message);
        } else {
            SdkStandardLoggers.REQUEST_LOGGER.debug(message);
        }
    }
}
