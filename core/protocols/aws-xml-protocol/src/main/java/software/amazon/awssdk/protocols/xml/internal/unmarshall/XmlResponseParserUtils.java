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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlDomParser;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

/**
 * Static methods to assist with parsing the response of AWS XML requests.
 */
@SdkInternalApi
public final class XmlResponseParserUtils {
    private XmlResponseParserUtils() {
    }

    /**
     * Parse an XML response if one is expected and available. If we are not expecting a payload, but the HTTP response
     * code shows an error then we will parse it anyway, as it should contain a serialized error.
     * @param sdkPojo the SDK builder object associated with the final response
     * @param response the HTTP response
     * @return A parsed XML document or an empty XML document if no payload/contents were found in the response.
     */
    public static XmlElement parse(SdkPojo sdkPojo, SdkHttpFullResponse response) {

        try {
            Optional<AbortableInputStream> responseContent = response.content();

            // In some cases the responseContent is present but empty, so when we are not expecting a body we should
            // not attempt to parse it even if the body appears to be present.
            if ((!response.isSuccessful() || hasPayloadMembers(sdkPojo)) && responseContent.isPresent() &&
                !contentLengthZero(response)) {
                return XmlDomParser.parse(responseContent.get());
            } else {
                return XmlElement.empty();
            }
        } catch (RuntimeException e) {
            if (response.isSuccessful()) {
                throw e;
            }

            return XmlElement.empty();
        }
    }

    private static boolean hasPayloadMembers(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields().stream()
                      .anyMatch(f -> f.location() == MarshallLocation.PAYLOAD);
    }

    private static boolean contentLengthZero(SdkHttpFullResponse response) {
        return response.firstMatchingHeader(CONTENT_LENGTH).map(l -> Long.parseLong(l) == 0).orElse(false);
    }
}
