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

package software.amazon.awssdk.http.pipeline.stages;

import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.RetryableException;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.SdkStandardLoggers;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.pipeline.RequestPipeline;

/**
 * Unmarshalls an HTTP response into either a successful response POJO, or into a (possibly modeled) exception. Returns a wrapper
 * {@link Response} object which may contain either the unmarshalled success POJO, or the unmarshalled exception.
 *
 * @param <OutputT> Type of successful unmarshalled POJO.
 */
@ReviewBeforeRelease("Should this be broken up? It's doing quite a lot...")
public class HandleResponseStage<OutputT> implements RequestPipeline<HttpResponse, Response<OutputT>> {

    private final HttpResponseHandler<OutputT> successResponseHandler;
    private final HttpResponseHandler<? extends SdkBaseException> errorResponseHandler;

    public HandleResponseStage(HttpResponseHandler<OutputT> successResponseHandler,
                               HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
        this.successResponseHandler = successResponseHandler;
        this.errorResponseHandler = errorResponseHandler;
    }

    @Override
    public Response<OutputT> execute(HttpResponse httpResponse, RequestExecutionContext context) throws Exception {
        boolean didRequestFail = true;
        try {
            Response<OutputT> response = handleResponse(httpResponse, context);
            didRequestFail = response.isFailure();
            return response;
        } finally {
            closeInputStreamIfNeeded(httpResponse, didRequestFail);
        }
    }

    private Response<OutputT> handleResponse(HttpResponse httpResponse,
                                             RequestExecutionContext context)
            throws IOException, InterruptedException {
        if (httpResponse.isSuccessful()) {
            OutputT response = handleSuccessResponse(httpResponse, context);
            return Response.fromSuccess(response, httpResponse);
        } else {
            return Response.fromFailure(handleErrorResponse(httpResponse, context), httpResponse);
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
    @SuppressWarnings("deprecation")
    private OutputT handleSuccessResponse(HttpResponse httpResponse, RequestExecutionContext context)
            throws IOException, InterruptedException {
        ProgressListener listener = context.requestConfig().getProgressListener();
        try {
            OutputT awsResponse;
            publishProgress(listener, ProgressEventType.HTTP_RESPONSE_STARTED_EVENT);
            awsResponse = successResponseHandler.handle(httpResponse, context.executionAttributes());
            publishProgress(listener, ProgressEventType.HTTP_RESPONSE_COMPLETED_EVENT);

            return awsResponse;
        } catch (IOException | InterruptedException | RetryableException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage =
                    "Unable to unmarshall response (" + e.getMessage() + "). Response Code: "
                    + httpResponse.getStatusCode() + ", Response Text: " + httpResponse.getStatusText();
            throw new SdkClientException(errorMessage, e);
        }
    }

    /**
     * Responsible for handling an error response, including unmarshalling the error response
     * into the most specific exception type possible, and throwing the exception.
     *
     * @throws IOException If any problems are encountering reading the error response.
     */
    private SdkBaseException handleErrorResponse(HttpResponse httpResponse,
                                                 RequestExecutionContext context)
            throws IOException, InterruptedException {
        try {
            SdkBaseException exception = errorResponseHandler.handle(httpResponse, context.executionAttributes());
            exception.fillInStackTrace();
            SdkStandardLoggers.REQUEST_LOGGER.debug(() -> "Received error response: " + exception);
            return exception;
        } catch (InterruptedException | IOException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("Unable to unmarshall error response (%s). " +
                                                "Response Code: %d, Response Text: %s", e.getMessage(),
                                                httpResponse.getStatusCode(), httpResponse.getStatusText());
            throw new SdkClientException(errorMessage, e);
        }
    }

    /**
     * Close the input stream if required.
     */
    private void closeInputStreamIfNeeded(HttpResponse httpResponse,
                                          boolean didRequestFail) throws IOException {
        final Optional<InputStream> inputStreamOptional =
                Optional.ofNullable(httpResponse)
                        // If no content no need to close
                        .map(HttpResponse::getContent)
                        // Always close on failed requests. Close on successful unless streaming operation.
                        .filter(i -> didRequestFail || !successResponseHandler.needsConnectionLeftOpen());
        if (inputStreamOptional.isPresent()) {
            try {
                inputStreamOptional.get().close();
            } catch (Exception e) {
                // We don't want failure to close to hide the original exception.
                if (!didRequestFail) {
                    throw e;
                }
            }
        }
    }

}
