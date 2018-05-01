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

package software.amazon.awssdk.core.internal.http.response;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.ErrorType;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttributes;
import software.amazon.awssdk.core.internal.http.ErrorCodeParser;
import software.amazon.awssdk.core.protocol.json.JsonContent;
import software.amazon.awssdk.core.runtime.http.JsonErrorMessageParser;
import software.amazon.awssdk.core.runtime.transform.JsonErrorUnmarshaller;
import software.amazon.awssdk.http.HttpStatusFamily;

@SdkInternalApi
public class JsonErrorResponseHandler implements HttpResponseHandler<SdkServiceException> {

    private static final Logger log = LoggerFactory.getLogger(JsonErrorResponseHandler.class);

    private final List<JsonErrorUnmarshaller> unmarshallers;
    private final ErrorCodeParser errorCodeParser;
    private final JsonErrorMessageParser errorMessageParser;
    private final JsonFactory jsonFactory;

    public JsonErrorResponseHandler(
            List<JsonErrorUnmarshaller> errorUnmarshallers,
            ErrorCodeParser errorCodeParser,
            JsonErrorMessageParser errorMessageParser,
            JsonFactory jsonFactory) {
        this.unmarshallers = errorUnmarshallers;
        this.errorCodeParser = errorCodeParser;
        this.errorMessageParser = errorMessageParser;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }

    @Override
    public SdkServiceException handle(HttpResponse response,
                                      ExecutionAttributes executionAttributes) throws Exception {

        JsonContent jsonContent = JsonContent.createJsonContent(response, jsonFactory);
        String errorCode = errorCodeParser.parseErrorCode(response, jsonContent);

        SdkServiceException exception = createException(errorCode, jsonContent);

        if (exception.errorMessage() == null) {
            exception.errorMessage(errorMessageParser.parseErrorMessage(response, jsonContent.getJsonNode()));
        }

        exception.errorCode(errorCode);
        exception.serviceName(executionAttributes.getAttribute(SdkExecutionAttributes.SERVICE_NAME));
        exception.statusCode(response.getStatusCode());
        exception.errorType(getErrorType(response.getStatusCode()));
        exception.rawResponse(jsonContent.getRawContent());
        exception.requestId(getRequestIdFromHeaders(response.getHeaders()));
        exception.headers(response.getHeaders());

        return exception;
    }

    /**
     * Create an SdkServiceException using the chain of unmarshallers. This method will never
     * return null, it will always return a valid SdkServiceException
     *
     * @param errorCode   Error code to find an appropriate unmarshaller
     * @param jsonContent JsonContent of HTTP response
     * @return SdkServiceException
     */
    private SdkServiceException createException(String errorCode, JsonContent jsonContent) {
        SdkServiceException exception = unmarshallException(errorCode, jsonContent);
        if (exception == null) {
            exception = new SdkServiceException(
                    "Unable to unmarshall exception response with the unmarshallers provided");
        }
        return exception;
    }

    private SdkServiceException unmarshallException(String errorCode, JsonContent jsonContent) {
        for (JsonErrorUnmarshaller unmarshaller : unmarshallers) {
            if (unmarshaller.matchErrorCode(errorCode)) {
                try {
                    return unmarshaller.unmarshall(jsonContent.getJsonNode());
                } catch (Exception e) {
                    log.debug("Unable to unmarshall exception content", e);
                    return null;
                }
            }
        }
        return null;
    }

    private ErrorType getErrorType(int statusCode) {
        return HttpStatusFamily.of(statusCode) == HttpStatusFamily.SERVER_ERROR ? ErrorType.SERVICE : ErrorType.CLIENT;
    }

    private String getRequestIdFromHeaders(Map<String, String> headers) {
        for (Entry<String, String> headerEntry : headers.entrySet()) {
            if (headerEntry.getKey().equalsIgnoreCase(X_AMZN_REQUEST_ID_HEADER)) {
                return headerEntry.getValue();
            }
        }
        return null;
    }

}
