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

package software.amazon.awssdk.awscore.http.response;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonErrorUnmarshaller;
import software.amazon.awssdk.awscore.internal.protocol.json.ErrorCodeParser;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.JsonErrorResponseHandler;
import software.amazon.awssdk.core.internal.protocol.json.ErrorMessageParser;
import software.amazon.awssdk.core.internal.protocol.json.JsonContent;

/**
 * Implementation of HttpResponseHandler that handles a error response from AWS
 * services and unmarshalls the result using a JSON unmarshaller.
 */
@SdkInternalApi
public class AwsJsonErrorResponseHandler extends JsonErrorResponseHandler<AwsServiceException> {

    private final List<AwsJsonErrorUnmarshaller> unmarshallers;
    private final ErrorCodeParser errorCodeParser;
    private final ErrorMessageParser errorMessageParser;
    private final JsonFactory jsonFactory;

    public AwsJsonErrorResponseHandler(List<AwsJsonErrorUnmarshaller> errorUnmarshallers,
                                       ErrorCodeParser errorCodeParser,
                                       ErrorMessageParser errorMessageParser,
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
    public AwsServiceException handle(HttpResponse response,
                                      ExecutionAttributes executionAttributes) throws Exception {

        JsonContent jsonContent = JsonContent.createJsonContent(response, jsonFactory);
        String errorCode = errorCodeParser.parseErrorCode(response, jsonContent);
        AwsServiceException exception = createException(errorCode, jsonContent);

        if (exception.errorMessage() == null) {
            exception.errorMessage(errorMessageParser.parseErrorMessage(response, jsonContent.getJsonNode()));
        }

        exception.errorCode(errorCode);
        exception.serviceName(executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME));
        exception.statusCode(response.getStatusCode());
        exception.errorType(getErrorType(response.getStatusCode()));
        exception.rawResponse(jsonContent.getRawContent());
        exception.requestId(getRequestIdFromHeaders(response.getHeaders()));
        exception.headers(response.getHeaders());

        return exception;
    }

    /**
     * Create an {@link AwsServiceException} using the chain of unmarshallers. This method will never
     * return null, it will always return a valid SdkServiceException
     *
     * @param errorCode Error code to find an appropriate unmarshaller
     * @param jsonContent JsonContent of HTTP response
     * @return SdkServiceException
     */
    private AwsServiceException createException(String errorCode, JsonContent jsonContent) {
        return unmarshallers.stream()
                            .filter(u -> u.matchErrorCode(errorCode))
                            .findFirst()
                            .map(u -> safeUnmarshall(jsonContent, u))
                            .orElseGet(this::createUnknownException);
    }

    @Override
    protected AwsServiceException createUnknownException() {
        return new AwsServiceException(
            "Unable to unmarshall exception response with the unmarshallers provided");
    }
}
