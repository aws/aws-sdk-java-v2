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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.io.Writer;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.w3c.dom.Document;
import software.amazon.awssdk.utils.StringInputStream;

public final class XmlAsserts {

    private static final DocumentBuilder DOCUMENT_BUILDER = getDocumentBuilder();

    private XmlAsserts() {
    }

    private static DocumentBuilder getDocumentBuilder() {
        DocumentBuilderFactory dbf = newSecureDocumentBuilderFactory();
        try {
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertXmlEquals(String expectedXml, String actualXml) {
        try {
            doAssertXmlEquals(expectedXml, actualXml);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private static void doAssertXmlEquals(String expectedXml, String actualXml) throws Exception {
        Diff diff = new Diff(expectedXml, actualXml);
        diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
        if (!diff.similar()) {
            fail("\nExpected the following XML\n" + formatXml(expectedXml) +
                 "\nbut actual XML was\n\n" +
                 formatXml(actualXml));
        }
    }

    private static String formatXml(String xmlDocumentString) throws Exception {
        return formatXml(DOCUMENT_BUILDER.parse(new StringInputStream(xmlDocumentString)));
    }

    private static String formatXml(Document xmlDocument) throws Exception {
        Transformer transformer = newSecureTransformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(xmlDocument);
        transformer.transform(source, result);
        try (Writer writer = result.getWriter()) {
            return writer.toString();
        }
    }

    private static DocumentBuilderFactory newSecureDocumentBuilderFactory() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setXIncludeAware(false);
        docFactory.setExpandEntityReferences(false);
        trySetFeature(docFactory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(docFactory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(docFactory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(docFactory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetAttribute(docFactory, "http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        trySetAttribute(docFactory, "http://javax.xml.XMLConstants/property/accessExternalSchema", "");
        return docFactory;
    }

    private static TransformerFactory newSecureTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        trySetAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        trySetAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void trySetAttribute(DocumentBuilderFactory factory, String feature, String value) {
        try {
            factory.setAttribute(feature, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void trySetAttribute(TransformerFactory factory, String feature, Object value) {
        try {
            factory.setAttribute(feature, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
