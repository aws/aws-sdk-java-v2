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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Unmarshalls an HTTP response into either a successful response POJO, or into a (possibly modeled) exception based
 * on a predicate that the unmarshalled response can be tested against. Returns a wrapper {@link Response} object which
 * may contain either the unmarshalled success POJO, or the unmarshalled exception.
 *
 * @param <OutputT> Type of successful unmarshalled POJO.
 */
@SdkInternalApi
public class AwsXmlPredicatedResponseHandler<OutputT> implements HttpResponseHandler<Response<OutputT>> {
    private static final Logger log = LoggerFactory.getLogger(AwsXmlPredicatedResponseHandler.class);

    private final Function<SdkHttpFullResponse, SdkPojo> pojoSupplier;
    private final Function<AwsXmlUnmarshallingContext, OutputT> successResponseTransformer;
    private final Function<AwsXmlUnmarshallingContext, ? extends SdkException> errorResponseTransformer;
    private final Function<AwsXmlUnmarshallingContext, AwsXmlUnmarshallingContext> decorateContextWithError;
    private final boolean needsConnectionLeftOpen;

    /**
     * Standard constructor
     * @param pojoSupplier A method that supplies an empty builder of the correct type
     * @param successResponseTransformer A function that can unmarshall a response object from parsed XML
     * @param errorResponseTransformer A function that can unmarshall an exception object from parsed XML
     * @param decorateContextWithError A function that determines if the response was an error or not
     * @param needsConnectionLeftOpen true if the underlying connection should not be closed once parsed
     */
    public AwsXmlPredicatedResponseHandler(
        Function<SdkHttpFullResponse, SdkPojo> pojoSupplier,
        Function<AwsXmlUnmarshallingContext, OutputT> successResponseTransformer,
        Function<AwsXmlUnmarshallingContext, ? extends SdkException> errorResponseTransformer,
        Function<AwsXmlUnmarshallingContext, AwsXmlUnmarshallingContext> decorateContextWithError,
        boolean needsConnectionLeftOpen) {

        this.pojoSupplier = pojoSupplier;
        this.successResponseTransformer = successResponseTransformer;
        this.errorResponseTransformer = errorResponseTransformer;
        this.decorateContextWithError = decorateContextWithError;
        this.needsConnectionLeftOpen = needsConnectionLeftOpen;
    }

    /**
     * Handle a response
     * @param httpResponse The HTTP response object
     * @param executionAttributes The attributes attached to this particular execution.
     * @return A wrapped response object with the unmarshalled result in it.
     */
    @Override
    public Response<OutputT> handle(SdkHttpFullResponse httpResponse, ExecutionAttributes executionAttributes) {
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
                                             ExecutionAttributes executionAttributes) {

        AwsXmlUnmarshallingContext parsedResponse = parseResponse(httpResponse, executionAttributes);
        parsedResponse = decorateContextWithError.apply(parsedResponse);

        if (parsedResponse.isResponseSuccess()) {
            OutputT response = handleSuccessResponse(parsedResponse);
            return Response.<OutputT>builder().httpResponse(httpResponse)
                                              .response(response)
                                              .isSuccess(true)
                                              .build();
        } else {
            return Response.<OutputT>builder().httpResponse(httpResponse)
                                              .exception(handleErrorResponse(parsedResponse))
                                              .isSuccess(false)
                                              .build();
        }
    }

    private AwsXmlUnmarshallingContext parseResponse(SdkHttpFullResponse httpFullResponse,
                                                     ExecutionAttributes executionAttributes) {
        XmlElement document = XmlResponseParserUtils.parse(pojoSupplier.apply(httpFullResponse), httpFullResponse);

        return AwsXmlUnmarshallingContext.builder()
                                         .parsedXml(document)
                                         .executionAttributes(executionAttributes)
                                         .sdkHttpFullResponse(httpFullResponse)
                                         .build();
    }

    /**
     * Handles a successful response from a service call by unmarshalling the results using the
     * specified response handler.
     *
     * @return The contents of the response, unmarshalled using the specified response handler.
     */
    private OutputT handleSuccessResponse(AwsXmlUnmarshallingContext parsedResponse) {
        try {
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received successful response: "
                                                         + parsedResponse.sdkHttpFullResponse().statusCode());
            return successResponseTransformer.apply(parsedResponse);
        } catch (RetryableException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof SdkException && ((SdkException) e).retryable()) {
                throw (SdkException) e;
            }

            String errorMessage =
                    "Unable to unmarshall response (" + e.getMessage() + "). Response Code: "
                    + parsedResponse.sdkHttpFullResponse().statusCode() + ", Response Text: "
                    + parsedResponse.sdkHttpFullResponse().statusText().orElse(null);
            throw SdkClientException.builder().message(errorMessage).cause(e).build();
        }
    }

    /**
     * Responsible for handling an error response, including unmarshalling the error response
     * into the most specific exception type possible, and throwing the exception.
     */
    private SdkException handleErrorResponse(AwsXmlUnmarshallingContext parsedResponse) {
        try {
            SdkException exception = errorResponseTransformer.apply(parsedResponse);
            exception.fillInStackTrace();
            SdkStandardLogger.REQUEST_LOGGER.debug(() -> "Received error response: " + exception);
            return exception;
        } catch (Exception e) {
            String errorMessage = String.format("Unable to unmarshall error response (%s). " +
                                                "Response Code: %d, Response Text: %s", e.getMessage(),
                                                parsedResponse.sdkHttpFullResponse().statusCode(),
                                                parsedResponse.sdkHttpFullResponse().statusText().orElse("null"));
            throw SdkClientException.builder().message(errorMessage).cause(e).build();
        }
    }

    /**
     * Close the input stream if required.
     */
    private void closeInputStreamIfNeeded(SdkHttpFullResponse httpResponse,
                                          boolean didRequestFail) {
        // Always close on failed requests. Close on successful requests unless it needs connection left open
        if (didRequestFail || !needsConnectionLeftOpen) {
            Optional.ofNullable(httpResponse)
                    .flatMap(SdkHttpFullResponse::content) // If no content, no need to close
                    .ifPresent(s -> IoUtils.closeQuietly(s, log));
        }
    }
}
