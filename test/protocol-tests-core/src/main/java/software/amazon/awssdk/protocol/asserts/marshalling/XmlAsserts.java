/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.core.util.StringInputStream;

public final class XmlAsserts {

    private static final DocumentBuilder DOCUMENT_BUILDER = getDocumentBuilder();

    private XmlAsserts() {
    }

    private static DocumentBuilder getDocumentBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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
            fail(String.format("\nExpected the following XML\n" + formatXml(expectedXml) +
                               "\nbut actual XML was\n\n" +
                               formatXml(actualXml)));
        }
    }

    private static String formatXml(String xmlDocumentString) throws Exception {
        return formatXml(DOCUMENT_BUILDER.parse(new StringInputStream(xmlDocumentString)));
    }

    private static String formatXml(Document xmlDocument) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(xmlDocument);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }
}
