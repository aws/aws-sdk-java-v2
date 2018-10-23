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

package software.amazon.awssdk.protocols.query.internal.unmarshall;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Parses an XML document into a simple DOM like structure, {@link XmlElement}.
 */
@SdkInternalApi
public final class XmlDomParser {

    private static final ThreadLocal<XMLInputFactory> FACTORY = ThreadLocal.withInitial(XMLInputFactory::newInstance);

    private XmlDomParser() {
    }

    public static XmlElement parse(InputStream inputStream) throws XMLStreamException {
        XMLEventReader reader = FACTORY.get().createXMLEventReader(inputStream);
        XMLEvent nextEvent;
        // Skip ahead to the first start element
        do {
            nextEvent = reader.nextEvent();
        } while (reader.hasNext() && !nextEvent.isStartElement());
        return parseElement((StartElement) nextEvent, reader);
    }

    /**
     * Parse an XML elemnt and any nested elements by recursively calling this method.
     *
     * @param startElement Start element object containing attributes and element name.
     * @param reader XML reader to get more events.
     * @return Parsed {@link XmlElement}.
     */
    private static XmlElement parseElement(StartElement startElement, XMLEventReader reader) throws XMLStreamException {
        XmlElement.Builder elementBuilder = XmlElement.builder()
                                                      .elementName(startElement.getName().getLocalPart())
                                                      .attributes(attributesToMap(startElement.getAttributes()));
        XMLEvent nextEvent;
        do {
            nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                elementBuilder.addChildElement(parseElement((StartElement) nextEvent, reader));
            } else if (nextEvent.isCharacters()) {
                elementBuilder.textContent(((Characters) nextEvent).getData());
            }
        } while (!nextEvent.isEndElement());
        return elementBuilder.build();
    }

    /**
     * Converts an iterator of {@link Attribute}s to a map.
     *
     * @param attributes Attribute iterator.
     * @return Map of attributes.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> attributesToMap(Iterator attributes) {
        Map<String, String> attributeMap = new HashMap<>();
        attributes.forEachRemaining(a -> {
            Attribute attr = (Attribute) a;
            attributeMap.put(attr.getName().getLocalPart(), attr.getValue());
        });
        return attributeMap;
    }

}
