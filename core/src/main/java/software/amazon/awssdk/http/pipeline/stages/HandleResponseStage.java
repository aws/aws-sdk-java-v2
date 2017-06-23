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
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Unmarshalls an HTTP response into either a successful response POJO, or into a (possibly modeled) exception. Returns a wrapper
 * {@link Response} object which may contain either the unmarshalled success POJO, or the unmarshalled exception.
 *
 * @param <OutputT> Type of successful unmarshalled POJO.
 */
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
        Optional<Response<OutputT>> response = Optional.empty();
        try {
            response = Optional.of(handleResponse(httpResponse, context));
        } finally {
            closeInputStreamIfNeeded(httpResponse, didRequestFail(response));
        }

        return response.orElseThrow(() -> new IllegalStateException("Response should not be null"));
    }

    private Response<OutputT> handleResponse(HttpResponse httpResponse,
                                             RequestExecutionContext context)
            throws IOException, InterruptedException {
        if (httpResponse.isSuccessful()) {
            return Response.fromSuccess(handleSuccessResponse(httpResponse, context), httpResponse);
        } else {
            return Response.fromFailure(handleErrorResponse(httpResponse), httpResponse);
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
        context.awsRequestMetrics().addProperty(AwsRequestMetrics.Field.StatusCode, httpResponse.getStatusCode());
        ProgressListener listener = context.requestConfig().getProgressListener();
        try {
            OutputT awsResponse;
            context.awsRequestMetrics().startEvent(AwsRequestMetrics.Field.ResponseProcessingTime);
            publishProgress(listener, ProgressEventType.HTTP_RESPONSE_STARTED_EVENT);
            try {
                awsResponse = successResponseHandler.handle(httpResponse);
            } finally {
                context.awsRequestMetrics().endEvent(AwsRequestMetrics.Field.ResponseProcessingTime);
            }
            publishProgress(listener, ProgressEventType.HTTP_RESPONSE_COMPLETED_EVENT);

            return awsResponse;
        } catch (IOException | InterruptedException e) {
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
    private SdkBaseException handleErrorResponse(HttpResponse httpResponse)
            throws IOException, InterruptedException {
        try {
            SdkBaseException exception = errorResponseHandler.handle(httpResponse);
            exception.fillInStackTrace();
            if (AmazonHttpClient.REQUEST_LOG.isDebugEnabled()) {
                AmazonHttpClient.REQUEST_LOG.debug("Received error response: " + exception);
            }
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
            inputStreamOptional.get().close();
        }
    }

    /**
     * Determines whether a request failed or not based on the response. If the response is an empty optional then
     * execution failed with some non-service error like an IOException or some other client error. If the response
     * is a fulfilled optional and {@link Response#isFailure()} is true then the execution failed with a service error
     * of some kind.
     *
     * @param response Optional of unmarshalled response.
     * @return True if the response was a failure. False if it was a success.
     */
    private boolean didRequestFail(Optional<Response<OutputT>> response) {
        return response.map(Response::isFailure).orElse(true);
    }
}
