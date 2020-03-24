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

package software.amazon.awssdk.core.internal.http;

import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Unmarshalls an HTTP response into either a successful response POJO, or into a (possibly modeled) exception based
 * on the status code of the HTTP response. Returns a wrapper {@link Response} object which may contain either the
 * unmarshalled success POJO, or the unmarshalled exception. Can be used with streaming or non-streaming requests.
 *
 * @param <OutputT> Type of successful unmarshalled POJO.
 */
@SdkInternalApi
public class CombinedResponseHandler<OutputT> implements HttpResponseHandler<Response<OutputT>> {
    private static final Logger log = LoggerFactory.getLogger(CombinedResponseHandler.class);

    private final HttpResponseHandler<OutputT> successResponseHandler;
    private final HttpResponseHandler<? extends SdkException> errorResponseHandler;

    public CombinedResponseHandler(HttpResponseHandler<OutputT> successResponseHandler,
                                   HttpResponseHandler<? extends SdkException> errorResponseHandler) {
        this.successResponseHandler = successResponseHandler;
        this.errorResponseHandler = errorResponseHandler;
    }

    @Override
    public Response<OutputT> handle(SdkHttpFullResponse httpResponse, ExecutionAttributes executionAttributes)
            throws Exception {

        boolean didRequestFail = true;
        try {
            Response<OutputT> response = handleResponse(httpResponse, executionAttributes);
            didRequestFail = !response.isSuccess();
            return response;
        } finally {
            closeInputStreamIfNeeded(httpResponse, didRequestFail);
        }
    }

    private Response<OutputT> handleResponse(SdkHttpFullResponse httpResponse,
                                             ExecutionAttributes executionAttributes)
            throws IOException, InterruptedException {

        if (httpResponse.isSuccessful()) {
            OutputT response = handleSuccessResponse(httpResponse, executionAttributes);
            return Response.<OutputT>builder().httpResponse(httpResponse)
                                              .response(response)
                                              .isSuccess(true)
                                              .build();
        } else {
            return Response.<OutputT>builder().httpResponse(httpResponse)
                                              .exception(handleErrorResponse(httpResponse, executionAttributes))
                                              .isSuccess(false)
                                              .build();
        }
    }

    /**
     * Handles a successful response from a service call by unmarshalling the results using the
     * specified response handler.
     *
     * @return The contents of the response, unmarshalled using the specified response handler.
     * @throws IOException If any problems were encountered reading the response contents from
     *                     the HTTP method object.
     */
    private OutputT handleSuccessResponse(SdkHttpFullResponse httpResponse, ExecutionAttributes executionAttributes)
            throws IOException, InterruptedException {
        try {
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received successful response: " + httpResponse.statusCode());
            return successResponseHandler.handle(httpResponse, executionAttributes);
        } catch (IOException | InterruptedException | RetryableException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof SdkException && ((SdkException) e).retryable()) {
                throw (SdkException) e;
            }

            String errorMessage =
                    "Unable to unmarshall response (" + e.getMessage() + "). Response Code: "
                    + httpResponse.statusCode() + ", Response Text: " + httpResponse.statusText().orElse(null);
            throw SdkClientException.builder().message(errorMessage).cause(e).build();
        }
    }

    /**
     * Responsible for handling an error response, including unmarshalling the error response
     * into the most specific exception type possible, and throwing the exception.
     *
     * @throws IOException If any problems are encountering reading the error response.
     */
    private SdkException handleErrorResponse(SdkHttpFullResponse httpResponse,
                                             ExecutionAttributes executionAttributes)
            throws IOException, InterruptedException {
        try {
            SdkException exception = errorResponseHandler.handle(httpResponse, executionAttributes);
            exception.fillInStackTrace();
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received error response: " + exception);
            return exception;
        } catch (InterruptedException | IOException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("Unable to unmarshall error response (%s). " +
                                                "Response Code: %d, Response Text: %s", e.getMessage(),
                                                httpResponse.statusCode(), httpResponse.statusText().orElse("null"));
            throw SdkClientException.builder().message(errorMessage).cause(e).build();
        }
    }

    /**
     * Close the input stream if required.
     */
    private void closeInputStreamIfNeeded(SdkHttpFullResponse httpResponse,
                                          boolean didRequestFail) {
        // Always close on failed requests. Close on successful requests unless it needs connection left open
        if (didRequestFail || !successResponseHandler.needsConnectionLeftOpen()) {
            Optional.ofNullable(httpResponse)
                    .flatMap(SdkHttpFullResponse::content) // If no content, no need to close
                    .ifPresent(s -> IoUtils.closeQuietly(s, log));
        }
    }
}
