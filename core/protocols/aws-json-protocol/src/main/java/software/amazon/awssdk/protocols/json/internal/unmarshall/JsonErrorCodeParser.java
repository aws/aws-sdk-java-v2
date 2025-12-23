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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.ErrorCodeParser;
import software.amazon.awssdk.protocols.json.JsonContent;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class JsonErrorCodeParser implements ErrorCodeParser {

    /**
     * Services using AWS JSON 1.1 protocol with HTTP binding send the error code information in the
     * response headers, instead of the content. Package private for tests.
     */
    public static final String X_AMZN_ERROR_TYPE = "x-amzn-ErrorType";

    static final String ERROR_CODE_HEADER = ":error-code";

    static final String EXCEPTION_TYPE_HEADER = ":exception-type";

    static final List<String> ERROR_CODE_FIELDS = Collections.unmodifiableList(Arrays.asList("__type", "code"));

    /**
     * List of header keys that represent the error code sent by service.
     * Response should only contain one of these headers
     */
    private final List<String> errorCodeHeaders;
    private final List<String> errorCodeFieldNames;

    public JsonErrorCodeParser(String customErrorCodeFieldName) {
        this.errorCodeFieldNames = customErrorCodeFieldName == null ?
                                   ERROR_CODE_FIELDS : Collections.singletonList(customErrorCodeFieldName);
        this.errorCodeHeaders = Arrays.asList(X_AMZN_ERROR_TYPE, ERROR_CODE_HEADER, EXCEPTION_TYPE_HEADER);
    }

    /**
     * Parse the error code from the response.
     *
     * @return Error Code of exceptional response or null if it can't be determined
     */
    @Override
    public String parseErrorCode(SdkHttpFullResponse response, JsonContent jsonContent) {
        String errorCodeFromHeader = parseErrorCodeFromHeader(response);
        if (errorCodeFromHeader != null) {
            return errorCodeFromHeader;
        } else if (jsonContent != null) {
            return parseErrorCodeFromContents(jsonContent.getJsonNode());
        } else {
            return null;
        }
    }

    /**
     * Attempt to parse the error code from the response headers. Returns null if information is not
     * present in the header.
     */
    private String parseErrorCodeFromHeader(SdkHttpFullResponse response) {
        for (String errorCodeHeader : errorCodeHeaders) {
            Optional<String> errorCode = response.firstMatchingHeader(errorCodeHeader);

            if (errorCode.isPresent()) {
                if (X_AMZN_ERROR_TYPE.equals(errorCodeHeader)) {
                    return parseErrorCodeFromXAmzErrorType(errorCode.get());
                }

                return errorCode.get();
            }
        }

        return null;
    }

    private String parseErrorCodeFromXAmzErrorType(String headerValue) {
        if (headerValue != null) {
            int separator = headerValue.indexOf(':');
            if (separator != -1) {
                headerValue = headerValue.substring(0, separator);
            }
        }
        return headerValue;
    }

    /**
     * Attempt to parse the error code from the response content. Returns null if information is not
     * present in the content. Codes are expected to be in the form <b>"typeName"</b> or
     * <b>"prefix#typeName"</b> Examples : "AccessDeniedException",
     * "software.amazon.awssdk.dynamodb.v20111205#ProvisionedThroughputExceededException"
     */
    private String parseErrorCodeFromContents(JsonNode jsonContents) {
        if (jsonContents == null) {
            return null;
        }
        JsonNode errorCodeField = errorCodeFieldNames.stream()
                                                     .map(jsonContents::field)
                                                     .filter(Optional::isPresent)
                                                     .map(Optional::get)
                                                     .findFirst()
                                                     .orElse(null);
        if (errorCodeField == null) {
            return null;
        }
        String code = errorCodeField.text();
        // now extract the error code from the field contents following the smithy defined rules:
        // 1) If a : character is present, then take only the contents before the first : character in the value.
        // 2) If a # character is present, then take only the contents after the first # character in the value.
        // see: https://smithy.io/2.0/aws/protocols/aws-json-1_1-protocol.html#operation-error-serialization
        int start = 0;
        int end = code.length();

        // 1 - everything before the first ':'
        int colonIndex = code.indexOf(':');
        if (colonIndex >= 0) {
            end = colonIndex;
        }

        // 2 - everything after the first '#'
        int hashIndex = code.indexOf('#');
        if (hashIndex >= 0 && hashIndex + 1 < end) {
            start = hashIndex + 1;
        }

        return code.substring(start, end);
    }

    public static String parseErrorCode(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int start = 0;
        int end = value.length();

        // Step 1: everything before the first ':'
        int colonIndex = value.indexOf(':');
        if (colonIndex >= 0) {
            end = colonIndex;
        }

        // Step 2: everything after the first '#'
        int hashIndex = value.indexOf('#');
        if (hashIndex >= 0 && hashIndex + 1 < end) {
            start = hashIndex + 1;
        }

        // Fast-path: return original string if unchanged
        if (start == 0 && end == value.length()) {
            return value;
        }

        return value.substring(start, end);
    }
}
