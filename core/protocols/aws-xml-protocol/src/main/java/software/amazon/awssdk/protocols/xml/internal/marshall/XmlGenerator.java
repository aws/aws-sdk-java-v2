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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import java.io.StringWriter;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Wrapper around the {@link XmlWriter} for marshalling requests for XML protocol
 */
@SdkInternalApi
public final class XmlGenerator {

    private final StringWriter stringWriter;
    private final XmlWriter xmlWriter;

    private XmlGenerator(StringWriter stringWriter, XmlWriter xmlWriter) {
        this.stringWriter = stringWriter;
        this.xmlWriter = xmlWriter;
    }

    public static XmlGenerator create(String xmlns) {
        StringWriter stringWriter = new StringWriter();
        return new XmlGenerator(stringWriter, new XmlWriter(stringWriter, xmlns));
    }

    public XmlWriter xmlWriter() {
        return xmlWriter;
    }

    public StringWriter stringWriter() {
        return stringWriter;
    }

    public void startElement(String element) {
        xmlWriter.startElement(element);
    }

    /**
     * Start to write the element
     *
     * @param element the element to write
     * @param attributes the attributes
     */
    public void  startElement(String element, Map<String, String> attributes) {
        xmlWriter.startElement(element, attributes);
    }

    public void endElement() {
        xmlWriter.endElement();
    }
}
