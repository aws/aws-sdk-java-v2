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

package software.amazon.awssdk.core.internal.http;

import com.fasterxml.jackson.core.JsonFactory;

import java.util.List;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.protocol.json.JsonContent;
import software.amazon.awssdk.core.internal.protocol.json.SdkJsonErrorUnmarshaller;

/**
 * Default implementation of {@link JsonErrorResponseHandler} that handles an error response from a
 * service and unmarshalls the result using an JSON error unmarshaller.
 */
@SdkInternalApi
public class SdkJsonErrorResponseHandler extends JsonErrorResponseHandler<SdkServiceException> {

    private final List<SdkJsonErrorUnmarshaller> unmarshallers;
    private final JsonFactory jsonFactory;

    public SdkJsonErrorResponseHandler(
            List<SdkJsonErrorUnmarshaller> errorUnmarshallers,
            JsonFactory jsonFactory) {
        this.unmarshallers = errorUnmarshallers;
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

        SdkServiceException.Builder exception = createException(response.getStatusCode(), jsonContent).toBuilder();

        exception.statusCode(response.getStatusCode());
        exception.requestId(getRequestIdFromHeaders(response.getHeaders()));

        return exception.build();
    }

    /**
     * Create an SdkServiceException using the chain of unmarshallers. This method will never
     * return null, it will always return a valid exception.
     *
     * @param httpStatusCode Http status code to find an appropriate unmarshaller
     * @param jsonContent    JsonContent of HTTP response
     * @return Unmarshalled exception
     */
    private SdkServiceException createException(int httpStatusCode, JsonContent jsonContent) {
        return unmarshallers.stream()
                            .filter(u -> u.matches(httpStatusCode))
                            .findFirst()
                            .map(u -> safeUnmarshall(jsonContent, u))
                            .orElseGet(this::createUnknownException);
    }

    @Override
    protected SdkServiceException createUnknownException() {
        return SdkServiceException.builder()
                                  .message("Unable to unmarshall exception response with the unmarshallers provided")
                                  .build();
    }
}
