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

package software.amazon.awssdk.core.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.exception.Crc32MismatchException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContextImpl;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Default implementation of HttpResponseHandler that handles a successful response from a
 * service and unmarshalls the result using a JSON unmarshaller.
 *
 * @param <T> Indicates the type being unmarshalled by this response handler.
 */
@SdkProtectedApi
public class JsonResponseHandler<T> implements HttpResponseHandler<T> {
    private static final Logger log = Logger.loggerFor(JsonResponseHandler.class);

    private final JsonFactory jsonFactory;
    private final boolean needsConnectionLeftOpen;
    private final boolean isPayloadJson;
    private final Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> simpleTypeUnmarshallers;
    /**
     * The JSON unmarshaller to use when handling the response
     */
    private Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller;

    /**
     * Constructs a new response handler that will use the specified JSON unmarshaller to unmarshall
     * the service response and uses the specified response element path to find the root of the
     * business data in the service's response.
     *
     * @param responseUnmarshaller    The JSON unmarshaller to use on the response.
     * @param simpleTypeUnmarshallers List of unmarshallers to be used for scalar types.
     * @param jsonFactory             the json factory to be used for parsing the response.
     */
    public JsonResponseHandler(Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller,
                               Map<Class<?>, Unmarshaller<?, JsonUnmarshallerContext>> simpleTypeUnmarshallers,
                               JsonFactory jsonFactory, boolean needsConnectionLeftOpen,
                               boolean isPayloadJson) {
        this.responseUnmarshaller = paramNotNull(responseUnmarshaller, "responseUnmarshaller");

        this.needsConnectionLeftOpen = needsConnectionLeftOpen;
        this.isPayloadJson = isPayloadJson;

        this.simpleTypeUnmarshallers = paramNotNull(simpleTypeUnmarshallers, "simple type unmarshallers");
        this.jsonFactory = paramNotNull(jsonFactory, "JSONFactory");
    }


    /**
     * @see HttpResponseHandler#handle(SdkHttpFullResponse, ExecutionAttributes)
     */
    @Override
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Parsing service response JSON.");
        SdkStandardLogger.REQUEST_ID_LOGGER.debug(() -> X_AMZN_REQUEST_ID_HEADER + " : " +
                                                        response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                                                .orElse("not available"));

        JsonParser jsonParser = null;

        if (shouldParsePayloadAsJson()) {
            jsonParser = jsonFactory.createParser(response.content().orElse(null));
        }

        try {
            JsonUnmarshallerContext unmarshallerContext = new JsonUnmarshallerContextImpl(
                    jsonParser, simpleTypeUnmarshallers, response);

            T result = responseUnmarshaller.unmarshall(unmarshallerContext);

            // Make sure we read all the data to get an accurate CRC32 calculation.
            // See https://github.com/aws/aws-sdk-java/issues/1018
            if (shouldParsePayloadAsJson() && response.content().isPresent()) {
                IoUtils.drainInputStream(response.content().get());
            }

            SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Done parsing service response.");
            return result;
        } finally {
            if (shouldParsePayloadAsJson()) {
                try {
                    jsonParser.close();
                } catch (Crc32MismatchException e) {
                    // Throw back out the CRC exception
                    throw e;
                } catch (IOException e) {
                    log.warn(() -> "Error closing JSON parser.", e);
                }
            }
        }
    }

    @Override
    public final boolean needsConnectionLeftOpen() {
        return needsConnectionLeftOpen;
    }

    /**
     * @return True if the payload will be parsed as JSON, false otherwise.
     */
    private boolean shouldParsePayloadAsJson() {
        return !needsConnectionLeftOpen && isPayloadJson;
    }

}
