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

package software.amazon.awssdk.protocols.query;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Parses an XML document into a simple DOM like structure, {@link XmlElement}.
 */
@SdkProtectedApi
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
        return parseElement(nextEvent.asStartElement(), reader);
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
                elementBuilder.addChildElement(parseElement(nextEvent.asStartElement(), reader));
            } else if (nextEvent.isCharacters()) {
                elementBuilder.textContent(readText(reader, nextEvent.asCharacters().getData()));
            }
        } while (!nextEvent.isEndElement());
        return elementBuilder.build();
    }

    /**
     * Reads all characters until the next end element event.
     *
     * @param eventReader Reader to read from.
     * @param firstChunk Initial character data that's already been read.
     * @return String with all character data concatenated.
     */
    private static String readText(XMLEventReader eventReader, String firstChunk) throws XMLStreamException {
        StringBuilder sb = new StringBuilder(firstChunk);
        while (true) {
            XMLEvent event = eventReader.peek();
            if (event.isCharacters()) {
                eventReader.nextEvent();
                sb.append(event.asCharacters().getData());
            } else {
                return sb.toString();
            }
        }
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
