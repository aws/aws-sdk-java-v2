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

package software.amazon.awssdk.runtime.http.response;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.internal.Crc32MismatchException;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContextImpl;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.runtime.transform.VoidJsonUnmarshaller;
import software.amazon.awssdk.util.ValidationUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Default implementation of HttpResponseHandler that handles a successful response from an AWS
 * service and unmarshalls the result using a JSON unmarshaller.
 *
 * @param <T> Indicates the type being unmarshalled by this response handler.
 */
@SdkProtectedApi
@ReviewBeforeRelease("Metadata in base result has been broken. Fix this and deal with AwsResponseHandlerAdapter")
public class JsonResponseHandler<T> implements HttpResponseHandler<T> {

    /**
     * Shared logger for profiling information
     */
    private static final Log log = LogFactory.getLog("software.amazon.awssdk.request");
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
        /*
         * Even if the invoked operation just returns null, we still need an
         * unmarshaller to run so we can pull out response metadata.
         *
         * We might want to pass this in through the client class so that we
         * don't have to do this check here.
         */
        this.responseUnmarshaller =
                responseUnmarshaller != null ? responseUnmarshaller : new VoidJsonUnmarshaller<T>();

        this.needsConnectionLeftOpen = needsConnectionLeftOpen;
        this.isPayloadJson = isPayloadJson;

        this.simpleTypeUnmarshallers = ValidationUtils
                .assertNotNull(simpleTypeUnmarshallers, "simple type unmarshallers");
        this.jsonFactory = ValidationUtils.assertNotNull(jsonFactory, "JSONFactory");
    }


    /**
     * @see HttpResponseHandler#handle(HttpResponse)
     */
    public T handle(HttpResponse response) throws Exception {
        log.trace("Parsing service response JSON");

        JsonParser jsonParser = null;

        if (shouldParsePayloadAsJson()) {
            jsonParser = jsonFactory.createParser(response.getContent());
        }

        try {
            JsonUnmarshallerContext unmarshallerContext = new JsonUnmarshallerContextImpl(
                    jsonParser, simpleTypeUnmarshallers, response);
            registerAdditionalMetadataExpressions(unmarshallerContext);

            T result = responseUnmarshaller.unmarshall(unmarshallerContext);

            // Make sure we read all the data to get an accurate CRC32 calculation.
            // See https://github.com/aws/aws-sdk-java/issues/1018
            if (shouldParsePayloadAsJson() && response.getContent() != null) {
                IoUtils.drainInputStream(response.getContent());
            }

            log.trace("Done parsing service response");
            return result;
        } finally {
            if (shouldParsePayloadAsJson()) {
                try {
                    jsonParser.close();
                } catch (Crc32MismatchException e) {
                    // Throw back out the CRC exception
                    throw e;
                } catch (IOException e) {
                    log.warn("Error closing json parser", e);
                }
            }
        }
    }

    /**
     * Hook for subclasses to override in order to collect additional metadata from service
     * responses.
     *
     * @param unmarshallerContext The unmarshaller context used to configure a service's response
     *                            data.
     */
    protected void registerAdditionalMetadataExpressions(
            JsonUnmarshallerContext unmarshallerContext) {
    }

    public boolean needsConnectionLeftOpen() {
        return needsConnectionLeftOpen;
    }

    /**
     * @return True if the payload will be parsed as JSON, false otherwise.
     */
    private boolean shouldParsePayloadAsJson() {
        return !needsConnectionLeftOpen && isPayloadJson;
    }

}
