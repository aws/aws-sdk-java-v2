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
import java.util.LinkedList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;

@SdkInternalApi
public class SdkJsonErrorMessageParser implements ErrorMessageParser {

    private static final List<String> DEFAULT_ERROR_MESSAGE_LOCATIONS = Arrays
        .asList("message", "Message", "errorMessage");
    /**
     * Standard JSON Error Message Parser that checks for JSON fields in this order: 'message',
     * 'Message', 'errorMessage'
     */
    public static final SdkJsonErrorMessageParser DEFAULT_ERROR_MESSAGE_PARSER = new SdkJsonErrorMessageParser(
        DEFAULT_ERROR_MESSAGE_LOCATIONS);

    private final List<String> errorMessageJsonLocations;

    /**
     * @param errorMessageJsonLocations JSON field locations where the parser will attempt to
     * extract the error message from.
     */
    private SdkJsonErrorMessageParser(List<String> errorMessageJsonLocations) {
        this.errorMessageJsonLocations = new LinkedList<>(errorMessageJsonLocations);
    }

    /**
     * Parse the error message from the response.
     *
     * @return Error Code of exceptional response or null if it can't be determined
     */
    @Override
    public String parseErrorMessage(SdkHttpFullResponse httpResponse, SdkJsonNode jsonNode) {
        for (String field : errorMessageJsonLocations) {
            SdkJsonNode value = jsonNode.get(field);
            if (value != null) {
                return value.asText();
            }
        }
        return null;
    }

}
