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

package software.amazon.awssdk.core.runtime.http;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.http.HttpResponse;

@SdkProtectedApi
public class JsonErrorMessageParser {

    /**
     * x-amzn-error-message may be returned by RESTFUL services that do not send a response
     * payload (like in a HEAD request).
     */
    public static final String X_AMZN_ERROR_MESSAGE = "x-amzn-error-message";
    private static final List<String> DEFAULT_ERROR_MESSAGE_LOCATIONS = Arrays
            .asList("message", "Message", "errorMessage");
    /**
     * Standard JSON Error Message Parser that checks for JSON fields in this order: 'message',
     * 'Message', 'errorMessage'
     */
    public static final JsonErrorMessageParser DEFAULT_ERROR_MESSAGE_PARSER = new JsonErrorMessageParser(
            DEFAULT_ERROR_MESSAGE_LOCATIONS);

    private static final HttpResponse EMPTY_HTTP_RESPONSE = new HttpResponse(null);

    private final List<String> errorMessageJsonLocations;

    /**
     * @param errorMessageJsonLocations JSON field locations where the parser will attempt to
     *                                  extract the error message from.
     */
    public JsonErrorMessageParser(List<String> errorMessageJsonLocations) {
        this.errorMessageJsonLocations = new LinkedList<String>(errorMessageJsonLocations);
    }

    /**
     * @deprecated By {@link #parseErrorMessage(HttpResponse, JsonNode)}
     */
    @Deprecated
    public String parseErrorMessage(JsonNode jsonNode) {
        return parseErrorMessage(EMPTY_HTTP_RESPONSE, jsonNode);
    }

    /**
     * Parse the error message from the response.
     *
     * @return Error Code of exceptional response or null if it can't be determined
     */
    public String parseErrorMessage(HttpResponse httpResponse, JsonNode jsonNode) {
        // If X_AMZN_ERROR_MESSAGE is present, prefer that. Otherwise check the JSON body.
        final String headerMessage = httpResponse.getHeader(X_AMZN_ERROR_MESSAGE);
        if (headerMessage != null) {
            return headerMessage;
        }
        for (String field : errorMessageJsonLocations) {
            JsonNode value = jsonNode.get(field);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return null;
    }

}
