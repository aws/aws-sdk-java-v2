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

package software.amazon.awssdk.opensdk.internal.protocol;

import com.fasterxml.jackson.core.JsonFactory;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpMetadata;
import software.amazon.awssdk.opensdk.SdkErrorHttpMetadata;
import software.amazon.awssdk.opensdk.internal.BaseException;
import software.amazon.awssdk.protocol.json.JsonContent;
import software.amazon.awssdk.runtime.http.JsonErrorMessageParser;
import software.amazon.awssdk.util.ValidationUtils;

/**
 * Error response handler for API Gateway clients. API Gateway errors are identified by their HTTP
 * status code.
 */
@SdkInternalApi
public class ApiGatewayErrorResponseHandler implements HttpResponseHandler<SdkBaseException> {

    private static final Log LOG = LogFactory.getLog(ApiGatewayErrorResponseHandler.class);

    private final List<ApiGatewayErrorUnmarshaller> unmarshallers;
    private final JsonFactory jsonFactory;
    private final JsonErrorMessageParser messageParser = JsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER;

    public ApiGatewayErrorResponseHandler(List<ApiGatewayErrorUnmarshaller> errorUnmarshallers,
                                          JsonFactory jsonFactory) {
        this.unmarshallers = ValidationUtils
                .assertNotNull(errorUnmarshallers, "errorUnmarshallers");
        this.jsonFactory = ValidationUtils.assertNotNull(jsonFactory, "jsonFactory");
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }

    @Override
    public SdkBaseException handle(HttpResponse response) throws Exception {
        JsonContent jsonContent = JsonContent.createJsonContent(response, jsonFactory);
        BaseException exception = createException(response.getStatusCode(), jsonContent);

        exception.sdkHttpMetadata(new SdkErrorHttpMetadata(SdkHttpMetadata.from(response), jsonContent.getRawContent()));
        exception.setMessage(messageParser.parseErrorMessage(jsonContent.getJsonNode()));

        // Wrap in ASE, will be unwrapped when caught in the service client.
        return tryConvertException(exception);
    }

    public SdkBaseException tryConvertException(BaseException exception) {
        if (exception instanceof SdkBaseException) {
            return (SdkBaseException) exception;
        }

        if (exception instanceof Throwable) {
            throw new IllegalArgumentException("Attempted but failed to convert exception to SdkBaseException.",
                                               (Throwable) exception);
        } else {
            throw new IllegalArgumentException("Attempted but failed to convert type to throwable: " + exception.getClass());
        }
    }

    /**
     * Create an AmazonServiceException using the chain of unmarshallers. This method will never
     * return null, it will always return a valid exception.
     *
     * @param httpStatusCode Http status code to find an appropriate unmarshaller
     * @param jsonContent    JsonContent of HTTP response
     * @return Unmarshalled exception
     */
    private BaseException createException(int httpStatusCode, JsonContent jsonContent) {
        return unmarshallers.stream()
                            .filter(u -> u.matches(httpStatusCode))
                            .findFirst()
                            .map(u -> safeUnmarshall(jsonContent, u))
                            .orElseThrow(this::createUnknownException);
    }

    private BaseException safeUnmarshall(JsonContent jsonContent,
                                         ApiGatewayErrorUnmarshaller unmarshaller) {
        try {
            return unmarshaller.unmarshall(jsonContent.getJsonNode());
        } catch (Exception e) {
            LOG.info("Unable to unmarshall exception content", e);
            throw new SdkClientException(e.getCause());
        }
    }

    /**
     * The default unmarshaller should always work but if it doesn't we fall back to creating an
     * exception explicitly.
     */
    private SdkClientException createUnknownException() {
        return new SdkClientException(
                "Unable to unmarshall exception response with the unmarshallers provided");
    }

}
