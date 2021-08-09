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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;

@SdkInternalApi
public final class AwsJsonErrorMessageParser implements ErrorMessageParser {

    public static final ErrorMessageParser DEFAULT_ERROR_MESSAGE_PARSER =
        new AwsJsonErrorMessageParser(SdkJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER);

    /**
     * x-amzn-error-message may be returned by RESTFUL services that do not send a response
     * payload (like in a HEAD request).
     */
    private static final String X_AMZN_ERROR_MESSAGE = "x-amzn-error-message";

    /**
     * Error message header returned by event stream errors
     */
    private static final String EVENT_ERROR_MESSAGE = ":error-message";

    private SdkJsonErrorMessageParser errorMessageParser;

    /**
     * @param errorMessageJsonLocations JSON field locations where the parser will attempt to
     * extract the error message from.
     */
    public AwsJsonErrorMessageParser(SdkJsonErrorMessageParser errorMessageJsonLocations) {
        this.errorMessageParser = errorMessageJsonLocations;
    }

    /**
     * Parse the error message from the response.
     *
     * @return Error Code of exceptional response or null if it can't be determined
     */
    @Override
    public String parseErrorMessage(SdkHttpFullResponse httpResponse, SdkJsonNode jsonNode) {
        String headerMessage = httpResponse.firstMatchingHeader(X_AMZN_ERROR_MESSAGE).orElse(null);
        if (headerMessage != null) {
            return headerMessage;
        }

        String eventHeaderMessage = httpResponse.firstMatchingHeader(EVENT_ERROR_MESSAGE).orElse(null);
        if (eventHeaderMessage != null) {
            return eventHeaderMessage;
        }

        return errorMessageParser.parseErrorMessage(httpResponse, jsonNode);
    }

}
