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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlDomParser;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.utils.LookaheadInputStream;

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

            if (!responseContent.isPresent() ||
                (response.isSuccessful() && !hasPayloadMembers(sdkPojo)) ||
                getBlobTypePayloadMemberToUnmarshal(sdkPojo).isPresent()) {
                return XmlElement.empty();
            }

            // Make sure there is content in the stream before passing it to the parser.
            LookaheadInputStream content = new LookaheadInputStream(responseContent.get());
            if (content.peek() == -1) {
                return XmlElement.empty();
            }

            return XmlDomParser.parse(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            if (response.isSuccessful()) {
                throw e;
            }
            return XmlElement.empty();
        }
    }

    /**
     * Gets the Member which is a Payload and which is of Blob Type.
     * @param sdkPojo
     * @return Optional of SdkField member if member is Blob type payload else returns Empty.
     */
    public static Optional<SdkField<?>> getBlobTypePayloadMemberToUnmarshal(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields().stream()
                .filter(e -> isExplicitPayloadMember(e))
                .filter(f -> f.marshallingType() == MarshallingType.SDK_BYTES).findFirst();
    }

    private static boolean isExplicitPayloadMember(SdkField<?> f) {
        return f.containsTrait(PayloadTrait.class);
    }

    private static boolean hasPayloadMembers(SdkPojo sdkPojo) {
        return sdkPojo.sdkFields().stream()
                      .anyMatch(f -> f.location() == MarshallLocation.PAYLOAD);
    }
}
