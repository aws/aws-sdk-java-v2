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

import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

/**
 * A function that decorates a {@link AwsXmlUnmarshallingContext} that already contains the parsed XML of the
 * response body with parsed error XML if the HTTP response status indicates failure or a serialized error is found
 * in the XML body of a 'successful' response. This is a non-standard error handling behavior that is used by some
 * non-streaming S3 operations.
 */
@SdkInternalApi
public class DecorateErrorFromResponseBodyUnmarshaller
    implements Function<AwsXmlUnmarshallingContext, AwsXmlUnmarshallingContext> {

    private static final String ERROR_IN_SUCCESS_BODY_ELEMENT_NAME = "Error";

    private final Function<XmlElement, Optional<XmlElement>> errorRootLocationFunction;

    private DecorateErrorFromResponseBodyUnmarshaller(Function<XmlElement, Optional<XmlElement>> errorRootLocationFunction) {
        this.errorRootLocationFunction = errorRootLocationFunction;
    }

    /**
     * Constructs a function that can be used to decorate a parsed error from a response if one is found.
     * @param errorRootFunction A function that can be used to locate the root of the serialized error in the XML
     *                          body if the HTTP status code of the response indicates an error. This function is not
     *                          applied for HTTP responses that indicate success, instead the root of the document
     *                          will always be checked for an element tagged 'Error'.
     * @return An unmarshalling function that will decorate the unmarshalling context with a parsed error if one is
     * found in the response.
     */
    public static DecorateErrorFromResponseBodyUnmarshaller of(Function<XmlElement, Optional<XmlElement>> errorRootFunction) {
        return new DecorateErrorFromResponseBodyUnmarshaller(errorRootFunction);
    }

    @Override
    public AwsXmlUnmarshallingContext apply(AwsXmlUnmarshallingContext context) {
        Optional<XmlElement> parsedRootXml = Optional.ofNullable(context.parsedRootXml());

        if (!context.sdkHttpFullResponse().isSuccessful()) {
            // Request was non-2xx, defer to protocol handler for error root
            Optional<XmlElement> parsedErrorXml = parsedRootXml.flatMap(errorRootLocationFunction);
            return context.toBuilder().isResponseSuccess(false).parsedErrorXml(parsedErrorXml.orElse(null)).build();
        }

        // Check body to see if an error turned up there
        Optional<XmlElement> parsedErrorXml = parsedRootXml.isPresent() ?
            getErrorRootFromSuccessBody(context.parsedRootXml()) : Optional.empty();

        // Request had an HTTP success code, but an error was found in the body
        return parsedErrorXml.map(xmlElement -> context.toBuilder()
                                                       .isResponseSuccess(false)
                                                       .parsedErrorXml(xmlElement)
                                                       .build())
                             // Otherwise the response can be considered successful
                             .orElseGet(() -> context.toBuilder()
                                                     .isResponseSuccess(true)
                                                     .build());
    }

    private static Optional<XmlElement> getErrorRootFromSuccessBody(XmlElement document) {
        return ERROR_IN_SUCCESS_BODY_ELEMENT_NAME.equals(document.elementName()) ?
            Optional.of(document) : Optional.empty();
    }
}
